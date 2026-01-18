package job_processing_platform.helpers;

import job_processing_platform.interfaces.JobHandler;

import java.util.List;

public final class JobHandlerValidator {

    private JobHandlerValidator() {
    }

    public static void validate(JobHandler handler) {
        if (handler.identify() == null) {
            throw new IllegalStateException("Job identify() cannot be empty");
        }

        if (handler.retries() < 0) {
            throw new IllegalStateException(
                    "Retries cannot be negative for " + handler.identify()
            );
        }

        if (handler.retries() > 5) {
            throw new IllegalStateException(
                    "Retries cannot be more than 5 for " + handler.identify()
            );
        }

        List<String> backoff = handler.backoff();

        if (handler.retries() > 0 && (backoff == null || backoff.isEmpty())) {
            throw new IllegalStateException(
                    "Backoff must be defined when retries > 0 for " + handler.identify()
            );
        }

        if (backoff != null && backoff.size() != handler.retries()) {
            throw new IllegalStateException(
                    "Backoff array must be equal to retries for " + handler.identify()
            );
        }
    }
}