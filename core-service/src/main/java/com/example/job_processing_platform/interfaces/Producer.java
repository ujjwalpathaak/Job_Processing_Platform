package com.example.job_processing_platform.interfaces;

public interface Producer<T> {
    void publish(T message);
}