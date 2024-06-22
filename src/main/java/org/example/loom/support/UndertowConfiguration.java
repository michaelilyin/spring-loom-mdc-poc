package org.example.loom.support;

import org.example.loom.support.decorator.SpawnScopedValueDecorator;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;

@Configuration
public class UndertowConfiguration {
    @Bean
    public UndertowDeploymentInfoCustomizer undertowDeploymentInfoCustomizer() {
        // https://stackoverflow.com/a/77668981
        // https://github.com/spring-projects/spring-boot/issues/38819#issuecomment-2106126983
        // https://issues.redhat.com/browse/UNDERTOW-2309
        // https://issues.redhat.com/browse/UNDERTOW-2389
        return deploymentInfo -> deploymentInfo
                .setExecutor(new SpawnScopedValueExecutor());
    }
}

class SpawnScopedValueExecutor extends VirtualThreadTaskExecutor {
    @Override
    public void execute(Runnable task) {
        super.execute(new SpawnScopedValueDecorator(task));
    }
}