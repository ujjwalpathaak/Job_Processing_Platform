package job_processing_platform.handlers.log;

import job_processing_platform.dto.LogMessage;
import job_processing_platform.enums.LogHandlerType;
import job_processing_platform.enums.LogLevel;
import job_processing_platform.interfaces.LogHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Component
public class FileLogHandler implements LogHandler {
    private static final Path ERROR_LOG_FILE =
            Path.of("logs/error.log");

    private static final Path LOG_FILE =
            Path.of("logs/application.log");

    @Override
    public LogHandlerType identify() {
        return LogHandlerType.FILE;
    }

    public List<LogLevel> supportedLevels() {
        return List.of(LogLevel.TRACE, LogLevel.INFO, LogLevel.ERROR);
    }

    @Override
    public void handle(LogMessage message) {
        try {
            Path filePath = getFilePath(message.getLevel());

            Files.createDirectories(filePath.getParent());

            String logLine = format(message);

            Files.writeString(
                    filePath,
                    logLine,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            System.err.println("Failed to write log to file: " + e.getMessage());
        }
    }

    private Path getFilePath(LogLevel level) {
        return switch (level) {
            case ERROR -> ERROR_LOG_FILE;
            case DEBUG, TRACE, INFO, WARN -> LOG_FILE;
        };
    }

    private String format(LogMessage message) {
        return String.format(
                "[%s] [%s] [%s] %s%n",
                message.getId(),
                message.getTimestamp(),
                message.getLevel(),
                message.getMessage()
        );
    }
}