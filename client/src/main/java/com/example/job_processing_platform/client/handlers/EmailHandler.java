package com.example.job_processing_platform.client.handlers;

import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

@Component
public class EmailHandler implements JobHandler<String> {

    public String identify() {
        return "EMAIL_HANDLER";
    }

    @Override
    public void handle(JobMessage message) {
        System.out.println("Email Sent");
    }
}
