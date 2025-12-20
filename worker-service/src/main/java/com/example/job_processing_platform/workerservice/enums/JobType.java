package com.example.job_processing_platform.workerservice.enums;

public enum JobType {
    SEND_EMAIL,
    SEND_SMS;

    public static JobType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("JobType cannot be null");
        }

        switch (value.toUpperCase()) {
            case "SEND_EMAIL":
                return SEND_EMAIL;

            case "SEND_SMS":
                return SEND_SMS;

            default:
                throw new IllegalArgumentException("Unknown JobType: " + value);
        }
    }
}