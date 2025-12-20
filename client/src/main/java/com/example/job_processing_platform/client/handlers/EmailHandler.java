package com.example.job_processing_platform.client.handlers;

import com.example.job_processing_platform.workerservice.dto.ConsumerJobMessage;
import com.example.job_processing_platform.workerservice.interfaces.JobHandler;
import org.springframework.stereotype.Component;

@Component
public class EmailHandler implements JobHandler<String> {

    public String identify() {
        return "EMAIL_HANDLER";
    }

    @Override
    public void handle(ConsumerJobMessage message) {
        System.out.println("Email Sent");
    }
}
