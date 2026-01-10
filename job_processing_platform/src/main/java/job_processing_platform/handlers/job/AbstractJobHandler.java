package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.interfaces.JobHandler;

public abstract class AbstractJobHandler implements JobHandler {

    @Override
    public final void process(JobMessage message) {
        validate(message);

        try {
            beforeExecute(message);
            execute(message);
            afterExecute(message);
        } catch (Exception ex) {
            onFailure(message, ex);
            throw ex;
        }
    }

    // --- hooks ---
    protected void beforeExecute(JobMessage message) {
    }

    protected void afterExecute(JobMessage message) {
    }

    protected void onFailure(JobMessage message, Exception ex) {
    }

    protected void validate(JobMessage message) {
    }

    // --- business logic ---
    protected abstract void execute(JobMessage message);
}
