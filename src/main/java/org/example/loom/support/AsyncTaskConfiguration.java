package org.example.loom.support;

import org.example.loom.support.decorator.InheritScopedValueDecorator;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;

@Configuration
public class AsyncTaskConfiguration {
    @Bean
    @Primary
    AsyncTaskExecutor applicationTaskExecutorVirtualThreads(SimpleAsyncTaskExecutorBuilder builder) {
        return builder.taskDecorator(InheritScopedValueDecorator::new).build();
    }
}
