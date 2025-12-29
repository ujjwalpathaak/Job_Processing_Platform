package job_processing_platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job.platform")
public class RabbitProperties {

    private final Rabbit rabbit = new Rabbit();

    public Rabbit getRabbit() {
        return rabbit;
    }

    public static class Rabbit {

        private String standardExchange;
        private final Standard standard = new Standard();

        public String getStandardExchange() {
            return standardExchange;
        }

        public void setStandardExchange(String standardExchange) {
            this.standardExchange = standardExchange;
        }

        public Standard getStandard() {
            return standard;
        }
    }

    public static class Standard {

        private String queue;
        private String routingKey;

        private final Retry retry30s = new Retry();

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

        public Retry getRetry30s() {
            return retry30s;
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