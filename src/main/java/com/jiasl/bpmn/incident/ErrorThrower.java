package com.jiasl.bpmn.incident;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ErrorThrower implements JobHandler{
    private static final Logger LOG = LoggerFactory.getLogger(ErrorThrower.class);

    @Override
    @JobWorker(type = "errorService", timeout = 2592000000L)
    public void handle(final JobClient client, final ActivatedJob job){
        LOG.info("handle Job...");
        if(!(Boolean)job.getVariablesAsMap().get("IsGood")) {
            throw new RuntimeException("I deliberatly throwed this exception.");
        }
    }
}
