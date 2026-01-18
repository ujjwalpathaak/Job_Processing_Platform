package job_processing_platform.dto;

import job_processing_platform.enums.LogLevel;

import java.time.Instant;
import java.util.UUID;

public class LogMessage {

    private final String id;
    private final String message;
    private final LogLevel level;
    private final Instant timestamp;

    public LogMessage(String message, LogLevel level) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.message = message;
        this.level = level;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public LogLevel getLevel() {
        return level;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}