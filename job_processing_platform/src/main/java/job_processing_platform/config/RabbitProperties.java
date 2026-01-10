package job_processing_platform.config;

import job_processing_platform.enums.JobCategory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "job.platform")
public class RabbitProperties {

    private final Rabbit rabbit = new Rabbit();

    public Rabbit getRabbit() {
        return rabbit;
    }

    public static class Rabbit {

        private final Queue standard = new Queue();
        private final Queue critical = new Queue();
        private final Queue external = new Queue();
        private Map<JobCategory, String> exchanges = new HashMap<>();
        private Map<String, Retry> retries = new HashMap<>();

        public Map<JobCategory, String> getExchanges() {
            return exchanges;
        }

        public void setExchanges(Map<JobCategory, String> exchanges) {
            this.exchanges = exchanges;
        }

        public Queue getStandard() {
            return standard;
        }

        public Queue getCritical() {
            return critical;
        }

        public Queue getExternal() {
            return external;
        }

        public Map<String, Retry> getRetries() {
            return retries;
        }

        public void setRetries(Map<String, Retry> retries) {
            this.retries = retries;
        }
    }

    public static class Queue {

        private String queue;
        private String routingKey;
        private String dlq;
        private String dlqRoutingKey;

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getDlq() {
            return dlq;
        }

        public void setDlq(String dlq) {
            this.dlq = dlq;
        }

        public String getDlqRoutingKey() {
            return dlqRoutingKey;
        }

        public void setDlqRoutingKey(String dlqRoutingKey) {
            this.dlqRoutingKey = dlqRoutingKey;
        }
    }

    public static class Retry {

        private String queue;
        private String routingKey;
        private long ttl;

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public long getTtl() {
            return ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }
    }
}