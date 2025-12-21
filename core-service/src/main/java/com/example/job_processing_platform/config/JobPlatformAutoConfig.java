package com.example.job_processing_platform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(com.example.job_processing_platform.config.JobPlatformProperties.class)
public class JobPlatformAutoConfig {
}