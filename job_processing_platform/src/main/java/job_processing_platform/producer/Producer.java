package job_processing_platform.producer;

import job_processing_platform.enums.JobCategory;
import job_processing_platform.interfaces.Message;
import job_processing_platform.manager.QueueManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class Producer {

    private final RabbitTemplate rabbitTemplate;
    private final QueueManager queueManager;

    public Producer(RabbitTemplate rabbitTemplate, QueueManager queueManager) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueManager = queueManager;
    }

    public void publish(Message message, JobCategory jobCategory) {
        String exchange = queueManager.getExchange(jobCategory);
        String routingKey = queueManager.getRoutingKey(jobCategory);

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message
        );
    }
}