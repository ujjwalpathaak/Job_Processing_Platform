package com.example.job_processing_platform.interfaces;

public interface Consumer<T> {
    void consume(T message);
}