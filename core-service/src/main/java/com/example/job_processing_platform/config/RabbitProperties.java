package com.example.job_processing_platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job.platform")
public class RabbitProperties {

    private String serviceName;
    private int maxRetries;

    private final Rabbit rabbit = new Rabbit();

    // ================= TOP LEVEL =================

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

    // ================= RABBIT =================

    public static class Rabbit {

        /**
         * Shared exchanges
         */
        private String exchange;
        private String retryExchange;
        private String dlqExchange;

        /**
         * Category-based queues
         */
        private final Category fast = new Category();
        private final Category slow = new Category();
        private final Category critical = new Category();

        /**
         * Logging
         */
        private String logQueue;
        private String logRoutingKey;

        // -------- exchanges --------

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRetryExchange() {
            return retryExchange;
        }

        public void setRetryExchange(String retryExchange) {
            this.retryExchange = retryExchange;
        }

        public String getDlqExchange() {
            return dlqExchange;
        }

        public void setDlqExchange(String dlqExchange) {
            this.dlqExchange = dlqExchange;
        }

        // -------- categories --------

        public Category getFast() {
            return fast;
        }

        public Category getSlow() {
            return slow;
        }

        public Category getCritical() {
            return critical;
        }

        // -------- logging --------

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

    // ================= CATEGORY =================

    public static class Category {

        private String queue;
        private String retryQueue;
        private String dlq;

        private String routingKey;
        private String retryRoutingKey;
        private String dlqRoutingKey;

        private long retryTtl;

        // -------- queues --------

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }

        public String getRetryQueue() {
            return retryQueue;
        }

        public void setRetryQueue(String retryQueue) {
            this.retryQueue = retryQueue;
        }

        public String getDlq() {
            return dlq;
        }

        public void setDlq(String dlq) {
            this.dlq = dlq;
        }

        // -------- routing keys --------

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getRetryRoutingKey() {
            return retryRoutingKey;
        }

        public void setRetryRoutingKey(String retryRoutingKey) {
            this.retryRoutingKey = retryRoutingKey;
        }

        public String getDlqRoutingKey() {
            return dlqRoutingKey;
        }

        public void setDlqRoutingKey(String dlqRoutingKey) {
            this.dlqRoutingKey = dlqRoutingKey;
        }

        // -------- retry --------

        public long getRetryTtl() {
            return retryTtl;
        }

        public void setRetryTtl(long retryTtl) {
            this.retryTtl = retryTtl;
        }
    }
}