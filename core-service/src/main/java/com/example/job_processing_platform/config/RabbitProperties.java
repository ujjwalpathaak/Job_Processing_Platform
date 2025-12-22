package com.example.job_processing_platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job.platform")
public class RabbitProperties {

    private String serviceName;
    private int maxRetries;

    private Rabbit rabbit = new Rabbit();

    public static class Rabbit {

        private String exchange;
        private String queue;
        private String routingKey;

        private String logQueue;
        private String logRoutingKey;

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

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

        public String getLogQueue() {
            return logQueue;
        }

        public void setLogQueue(String logQueue) {
            this.logQueue = logQueue;
        }

        public String getLogRoutingKey() {
            return logRoutingKey;
        }

        public void setLogRoutingKey(String logRoutingKey) {
            this.logRoutingKey = logRoutingKey;
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Rabbit getRabbit() {
        return rabbit;
    }
}