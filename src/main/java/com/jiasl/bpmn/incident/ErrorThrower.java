package com.jiasl.bpmn.incident;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.springframework.stereotype.Component;

@Component
public class ErrorThrower {
    @JobWorker(type="errorService")
    public void throwException(){
        throw new RuntimeException("I deliberatly throwed this exception.");
    }
}
