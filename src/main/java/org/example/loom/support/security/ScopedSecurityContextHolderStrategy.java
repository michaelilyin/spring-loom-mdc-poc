package org.example.loom.support.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.concurrent.atomic.AtomicReference;

public class ScopedSecurityContextHolderStrategy implements SecurityContextHolderStrategy {
    private static final ScopedValue<SecurityContextScopedValueHolder> SECURITY_CONTEXT = ScopedValue.newInstance();

    private static class SecurityContextScopedValueHolder {
        private final AtomicReference<SecurityContext> securityContext;

        SecurityContextScopedValueHolder(SecurityContext parentContext) {
            securityContext = new AtomicReference<>(parentContext);
        }

        public SecurityContext getSecurityContext() {
            return securityContext.get();
        }

        public void setSecurityContext(SecurityContext securityContext) {
            this.securityContext.set(securityContext);
        }

    }

    @Override
    public void clearContext() {
        // очистка вызывается в юнитах, поэтому не падаем если нет контекста (в юнитах его нет)
        if (SECURITY_CONTEXT.isBound()) {
            SECURITY_CONTEXT.get().setSecurityContext(null);
        }
    }

    @Override
    public SecurityContext getContext() {
        return retrieveSecurityContextScopedValueHolder().getSecurityContext();
    }

    @Override
    public void setContext(SecurityContext context) {
        retrieveSecurityContextScopedValueHolder().setSecurityContext(context);
    }

    @Override
    public SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }

    private SecurityContextScopedValueHolder retrieveSecurityContextScopedValueHolder() {
        if (SECURITY_CONTEXT.isBound()) {
            return SECURITY_CONTEXT.get();
        } else {
            throw new IllegalStateException("Security Context Scoped Value not bound");
        }
    }

    public static ScopedValue.Carrier initCarrier(ScopedValue.Carrier prev) {
        var holder = new SecurityContextScopedValueHolder(null);
        return bind(prev, holder);
    }

    public static ScopedValue.Carrier inheritCarrier(ScopedValue.Carrier prev) {
        if (SECURITY_CONTEXT.isBound()) {
            var currentValue = SECURITY_CONTEXT.get().getSecurityContext();
            var holder = new SecurityContextScopedValueHolder(currentValue);
            return bind(prev, holder);
        } else {
            return initCarrier(prev);
        }
    }

    private static ScopedValue.Carrier bind(ScopedValue.Carrier prev, SecurityContextScopedValueHolder holder) {
        if (prev == null) {
            return ScopedValue.where(SECURITY_CONTEXT, holder);
        } else {
            return prev.where(SECURITY_CONTEXT, holder);
        }
    }


}  