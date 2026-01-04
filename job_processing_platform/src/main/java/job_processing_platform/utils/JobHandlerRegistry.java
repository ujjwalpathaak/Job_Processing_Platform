package job_processing_platform.utils;

import jakarta.annotation.PostConstruct;
import job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JobHandlerRegistry {

    private final Map<String, JobHandler> registry = new HashMap<>();

    private final List<JobHandler> handlers;

    public JobHandlerRegistry(List<JobHandler> handlers) {
        this.handlers = handlers;
    }

    @PostConstruct
    public void init() {
        for (JobHandler handler : handlers) {
            String key = handler.identify();

            if (key == null || key.isBlank()) {
                throw new IllegalStateException(
                        "JobHandler identify() cannot be null/blank: "
                                + handler.getClass().getName()
                );
            }

            if (registry.containsKey(key)) {
                throw new IllegalStateException(
                        "Duplicate JobHandler for type: " + key
                                + " (" + handler.getClass().getName() + ")"
                );
            }

            registry.put(key, handler);
        }

        if (registry.isEmpty()) {
            throw new IllegalStateException("No JobHandlers registered!");
        }
    }

    public JobHandler get(String jobType) {
        JobHandler handler = registry.get(jobType);

        if (handler == null) {
            throw new IllegalStateException(
                    "No JobHandler found for type: " + jobType
            );
        }

        return handler;
    }
}