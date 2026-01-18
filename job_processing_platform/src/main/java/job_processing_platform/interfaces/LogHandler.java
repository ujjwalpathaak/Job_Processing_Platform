package job_processing_platform.interfaces;

import job_processing_platform.dto.LogMessage;
import job_processing_platform.enums.LogHandlerType;
import job_processing_platform.enums.LogLevel;

import java.util.List;

public interface LogHandler {
    LogHandlerType identify();

    List<LogLevel> supportedLevels();

    void handle(LogMessage message);
}