package job_processing_platform.factory;

import job_processing_platform.enums.JobHandlerType;
import job_processing_platform.handlers.job.*;
import job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

@Component
public class JobHandlerFactory {
    public JobHandler get(JobHandlerType handlerType) {
        return switch (handlerType) {
            case EMAIL -> new EmailJobHandler();
            case REPORT_GENERATION -> new ReportGenerationJobHandler();
            case NOTIFICATION_CLEANUP -> new NotificationCleanupJobHandler();
            case WEBHOOK_TRIGGER -> new WebhookTriggerJobHandler();
            case CRM_SYNC -> new CrmSyncJobHandler();
            case PAYMENT -> new PaymentProcessingJobHandler();
            case REFUND -> new RefundJobHandler();
        };
    }
}
