package com.example.job_processing_platform.jobservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JobPlatformProperties.class)
public class JobPlatformAutoConfig {
}