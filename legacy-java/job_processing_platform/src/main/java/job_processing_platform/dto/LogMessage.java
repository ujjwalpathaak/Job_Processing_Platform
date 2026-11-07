package job_processing_platform.dto;

import job_processing_platform.enums.LogLevel;

import java.time.Instant;
import java.util.UUID;

public class LogMessage {

    private final String id;
    private final String message;
    private final LogLevel level;
    private final Instant timestamp;
    private final String requestId;
    private final String event;
    private final String error;

    public LogMessage(String message, LogLevel level) {
        this(message, level, null, null, null);
    }

    public LogMessage(String message, LogLevel level, String requestId) {
        this(message, level, requestId, null, null);
    }

    public LogMessage(String message, LogLevel level, String requestId, String event, String error) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.message = message;
        this.level = level;
        this.timestamp = Instant.now();
        this.requestId = requestId;
        this.event = event;
        this.error = error;
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

    public String getRequestId() {
        return requestId;
    }

    public String getEvent() {
        return event;
    }

    public String getError() {
        return error;
    }
}