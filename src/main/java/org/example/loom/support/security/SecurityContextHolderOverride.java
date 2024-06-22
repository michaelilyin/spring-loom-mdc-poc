package org.example.loom.support.security;

import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextHolderOverride implements SpringApplicationRunListener {
    @Override
    public void starting(ConfigurableBootstrapContext bootstrapContext) {
        SecurityContextHolder.setContextHolderStrategy(new ScopedSecurityContextHolderStrategy());
    }
}
