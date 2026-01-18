package job_processing_platform.service;

import job_processing_platform.dto.LogMessage;
import job_processing_platform.enums.LogLevel;
import job_processing_platform.helpers.Template;
import job_processing_platform.interfaces.LogHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class log {
    private static final Map<LogLevel, List<LogHandler>> handlers =
            new EnumMap<>(LogLevel.class);

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

    private static void handle(String message, LogLevel level) {
        List<LogHandler> list = handlers.get(level);
        if (list == null || list.isEmpty()) return;

        LogMessage logMessage = new LogMessage(message, level);
        for (LogHandler handler : list) {
            handler.handle(logMessage);
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
}