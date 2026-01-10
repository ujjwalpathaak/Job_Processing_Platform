package job_processing_platform.registry;

import job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobHandlerRegistry
        extends AbstractHandlerRegistry<String, JobHandler> {

    public JobHandlerRegistry(List<JobHandler> handlers) {
        super(handlers);
    }

    @Override
    protected String identify(JobHandler handler) {
        return handler.identify();
    }

    @Override
    protected String registryName() {
        return "JobHandler";
    }
}