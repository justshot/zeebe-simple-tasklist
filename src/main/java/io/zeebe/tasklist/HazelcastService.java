package io.zeebe.tasklist;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.protocol.record.intent.JobIntent;
import io.zeebe.exporter.proto.Schema;
import io.zeebe.hazelcast.connect.java.ZeebeHazelcast;
import io.zeebe.tasklist.entity.HazelcastConfig;
import io.zeebe.tasklist.repository.HazelcastConfigRepository;
import io.zeebe.tasklist.repository.TaskRepository;
import io.zeebe.tasklist.view.NotificationService;
import java.time.Duration;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HazelcastService {

  private static final Logger LOG = LoggerFactory.getLogger(HazelcastService.class);

  @Value("${zeebe.client.worker.hazelcast.connection}")
  private String hazelcastConnection;

  @Value("${zeebe.client.worker.hazelcast.connectionTimeout}")
  private String hazelcastConnectionTimeout;

  @Autowired private NotificationService notificationService;
  @Autowired private TaskRepository taskRepository;
  @Autowired private HazelcastConfigRepository hazelcastConfigRepository;

  @Autowired private ZeebeClient zeebeClient;

  private ZeebeHazelcast hazelcast;

  @PostConstruct
  public void connect() {

    final var hazelcastConfig =
        hazelcastConfigRepository
            .findById("cfg")
            .orElseGet(
                () -> {
                  final var config = new HazelcastConfig();
                  config.setId("cfg");
                  config.setSequence(-1);
                  return config;
                });

    final ClientConfig clientConfig = new ClientConfig();
    clientConfig.getNetworkConfig().addAddress(hazelcastConnection);

    final var connectionRetryConfig =
        clientConfig.getConnectionStrategyConfig().getConnectionRetryConfig();
    connectionRetryConfig.setClusterConnectTimeoutMillis(
        Duration.parse(hazelcastConnectionTimeout).toMillis());

    try {
      LOG.info("Connecting to Hazelcast '{}'", hazelcastConnection);
      final HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

      final var builder =
          ZeebeHazelcast.newBuilder(hz)
              .addJobListener(this::handleJob)
                  .addProcessEventListener(this::handleProcess)
                  .addIncidentListener(this::handleIncident)
                  .addErrorListener(this::handleError)
              .postProcessListener(
                  sequence -> {
                    hazelcastConfig.setSequence(sequence);
                    hazelcastConfigRepository.save(hazelcastConfig);
                  });

      if (hazelcastConfig.getSequence() >= 0) {
        builder.readFrom(hazelcastConfig.getSequence());
      } else {
        builder.readFromHead();
      }

      hazelcast = builder.build();

    } catch (Exception e) {
      LOG.warn("Failed to connect to Hazelcast. Still works but no updates will be received.", e);
    }
  }

  private void handleProcess(Schema.ProcessEventRecord record) {
    LOG.debug("handleProcess");
    LOG.debug(record.toString());
  }

  private void handleIncident(Schema.IncidentRecord record) {
    LOG.info("handleIncident");
    LOG.info(record.toString());

    if(record.getMetadata().getIntent().equals("CREATED")) {
      zeebeClient.newSetVariablesCommand(record.getElementInstanceKey())
              .variables(Map.of("IsGood",true))
              .send()
              .join();
      zeebeClient.newUpdateRetriesCommand(record.getJobKey())
              .retries(3)
              .send()
              .join();
      zeebeClient.newResolveIncidentCommand(record.getMetadata().getKey())
              .send()
              .join();
    }
  }

  private void handleError(Schema.ErrorRecord record) {
    LOG.debug("handleError");
    LOG.debug(record.toString());
  }

  private void handleJob(Schema.JobRecord job) {
    LOG.debug("handleJob");
    LOG.debug(job.toString());
    if (isCanceled(job)) {

      taskRepository
          .findById(job.getMetadata().getKey())
          .ifPresent(
              task -> {
                taskRepository.delete(task);

                notificationService.sendTaskCanceled();
              });
    }
  }

  private boolean isCanceled(Schema.JobRecord job) {
    final String intent = job.getMetadata().getIntent();
    return JobIntent.CANCELED.name().equals(intent);
  }

  @PreDestroy
  public void close() throws Exception {
    if (hazelcast != null) {
      hazelcast.close();
    }
  }
}
