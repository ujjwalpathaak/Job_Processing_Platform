package job_processing_platform.bootstrap;

import job_processing_platform.interfaces.LogHandler;
import job_processing_platform.service.LogBatchingService;
import job_processing_platform.service.log;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogInitializer {
    public LogInitializer(List<LogHandler> handlers, LogBatchingService logBatchingService) {
        log.init(handlers);
        log.setBatchingService(logBatchingService);
    }
}
