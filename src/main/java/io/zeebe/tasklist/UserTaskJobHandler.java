package io.zeebe.tasklist;

import io.zeebe.client.api.clients.JobClient;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.response.JobHeaders;
import io.zeebe.client.api.subscription.JobHandler;
import io.zeebe.tasklist.entity.TaskEntity;
import io.zeebe.tasklist.repository.TaskRepository;
import io.zeebe.tasklist.view.FormField;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserTaskJobHandler implements JobHandler {

  private static final List<String> SUPPORTED_FIELD_TYPES =
      Arrays.asList("string", "number", "boolean");

  private final TaskDataSerializer serializer = new TaskDataSerializer();

  @Autowired private TaskRepository repository;

  @Override
  public void handle(JobClient client, ActivatedJob job) {

    final TaskEntity entity = new TaskEntity();

    entity.setKey(job.getKey());
    entity.setTimestamp(Instant.now().toEpochMilli());
    entity.setPayload(job.getPayload());

    final JobHeaders headers = job.getHeaders();
    final Map<String, Object> customHeaders = job.getCustomHeaders();

    final String name = (String) customHeaders.getOrDefault("name", headers.getElementId());
    entity.setName(name);

    final String description = (String) customHeaders.getOrDefault("description", "");
    entity.setDescription(description);

    Optional.ofNullable((String) customHeaders.get("formData"))
        .ifPresent(
            formData -> {
              validateFormData(formData);
              entity.setFormData(formData);
            });

    repository.save(entity);
  }

  private void validateFormData(String formData) {
    final List<FormField> formFields = serializer.readFormFields(formData);

    formFields.forEach(
        field -> {
          if (field.getKey() == null) {
            throw new RuntimeException("form-field must have a 'key'");
          }

          final String type = field.getType();
          if (!SUPPORTED_FIELD_TYPES.contains(type)) {
            throw new RuntimeException(
                String.format(
                    "type of form-field '%s' is not supported. Must be one of: %s",
                    type, SUPPORTED_FIELD_TYPES));
          }
        });
  }
}
