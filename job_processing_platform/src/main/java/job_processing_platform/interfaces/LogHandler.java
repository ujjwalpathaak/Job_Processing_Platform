package job_processing_platform.interfaces;

import job_processing_platform.enums.LogLevel;

public interface LogHandler {
    LogLevel identify();

    void handle(String message);
}