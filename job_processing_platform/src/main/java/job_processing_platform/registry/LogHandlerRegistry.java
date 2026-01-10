package job_processing_platform.registry;

import job_processing_platform.enums.LogLevel;
import job_processing_platform.interfaces.LogHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogHandlerRegistry
        extends AbstractHandlerRegistry<LogLevel, LogHandler> {

    public LogHandlerRegistry(List<LogHandler> handlers) {
        super(handlers);
    }

    @Override
    protected LogLevel identify(LogHandler handler) {
        return handler.identify();
    }

    @Override
    protected String registryName() {
        return "LogHandler";
    }
}