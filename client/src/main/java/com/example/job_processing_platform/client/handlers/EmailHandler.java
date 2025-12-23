package com.example.job_processing_platform.client.handlers;

import com.example.job_processing_platform.dto.JobMessage;
import com.example.job_processing_platform.enums.JobCategory;
import com.example.job_processing_platform.interfaces.JobDefinition;
import com.example.job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EmailHandler implements JobHandler {
    @Override
    public JobDefinition definition() {
        return new JobDefinition() {
            public String identify() {
                return "EMAIL_HANDLER";
            }

            public JobCategory category() {
                return JobCategory.FAST;
            }

            public int maxRetries() {
                return 3;
            }

            public Duration retryDelay() {
                return Duration.ofSeconds(5);
            }
        };
    }

    @Override
    public void handle(JobMessage message) {
        System.out.println("Email Sent");
    }
}
