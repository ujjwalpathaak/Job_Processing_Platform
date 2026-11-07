package job_processing_platform.service;

import jakarta.servlet.http.HttpServletRequest;
import job_processing_platform.dto.LogMessage;
import job_processing_platform.enums.LogLevel;
import job_processing_platform.helpers.Template;
import job_processing_platform.interfaces.LogHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public final class log {
    private static final Map<LogLevel, List<LogHandler>> handlers =
            new EnumMap<>(LogLevel.class);

    private static LogBatchingService batchingService;

    private log() {
    }

    public static void init(List<LogHandler> handlerList) {
        for (LogHandler handler : handlerList) {
            for (LogLevel level : handler.supportedLevels()) {
                handlers
                        .computeIfAbsent(level, k -> new ArrayList<>())
                        .add(handler);
            }
        }
    }

    public static void setBatchingService(LogBatchingService service) {
        batchingService = service;
    }

    private static void handle(String message, LogLevel level) {
        List<LogHandler> list = handlers.get(level);
        if (list == null || list.isEmpty()) return;

        String requestId = resolveRequestId();
        LogMessage logMessage = new LogMessage(message, level, requestId);
        for (LogHandler handler : list) {
            handler.handle(logMessage);
        }
        if (batchingService != null) {
            batchingService.addLog(logMessage);
        }
    }

    public static void info(String template, Object... args) {
        handle(Template.format(template, args), LogLevel.INFO);
    }

    public static void debug(String template, Object... args) {
        handle(Template.format(template, args), LogLevel.DEBUG);
    }

    public static void trace(String template, Object... args) {
        handle(Template.format(template, args), LogLevel.TRACE);
    }

    public static void warn(String template, Object... args) {
        handle(Template.format(template, args), LogLevel.WARN);
    }

    public static void error(String template, Object... args) {
        handle(Template.format(template, args), LogLevel.ERROR);
    }

    private static String resolveRequestId() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            Object requestId = request.getAttribute("requestId");
            if (requestId instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}