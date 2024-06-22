package org.example.loom.support.mdc;

import ch.qos.logback.classic.util.LogbackMDCAdapter;
import org.slf4j.spi.MDCAdapter;

import java.util.*;

public class ScopedMdcAdapter implements MDCAdapter {
    private static class SubtaskContext implements MDCAdapter {

        private final DelegatingMap scopedMap;

        public SubtaskContext(Map<String, String> parentContext) {
            scopedMap = new DelegatingMap(parentContext);
        }

        public void put(String key, String val) {
            scopedMap.put(key, val);
        }

        public String get(String key) {
            return scopedMap.get(key);
        }

        @Override
        public void remove(String key) {
           scopedMap.remove(key);
        }

        @Override
        public void clear() {
            scopedMap.clear();
        }

        @Override
        public Map<String, String> getCopyOfContextMap() {
            return Collections.unmodifiableMap(scopedMap);
        }

        @Override
        public void setContextMap(Map<String, String> contextMap) {
            scopedMap.clear();
            scopedMap.putAll(contextMap);
        }

        @Override
        public void pushByKey(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String popByKey(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Deque<String> getCopyOfDequeByKey(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clearDequeByKey(String key) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Stores {@link SubtaskContext} MDC context bound to a current thread
     * by {@link ScopedValue#runWhere(ScopedValue, Object, Runnable)} method.
     */
    private static final ScopedValue<SubtaskContext> SUBTASK_CONTEXT = ScopedValue.newInstance();

    public static ScopedValue.Carrier initCarrier(ScopedValue.Carrier prev) {
        var context = new SubtaskContext(Map.of());
        return bind(prev, context);
    }

    public static ScopedValue.Carrier inheritCarrier(ScopedValue.Carrier prev) {
        if (SUBTASK_CONTEXT.isBound()) {
            var context = new SubtaskContext(SUBTASK_CONTEXT.get().scopedMap);
            return bind(prev, context);
        } else {
            return initCarrier(prev);
        }
    }

    private static ScopedValue.Carrier bind(ScopedValue.Carrier prev, SubtaskContext context) {
        if (prev == null) {
            return ScopedValue.where(SUBTASK_CONTEXT, context);
        } else {
            return prev.where(SUBTASK_CONTEXT, context);
        }
    }

    /**
     * Root {@link MDCAdapter}. It is used when {@link #SUBTASK_CONTEXT} is not bound to current thread
     */
    private MDCAdapter rootContext = new LogbackMDCAdapter();

    @Override
    public void put(String key, String val) {
        getCurrentContext().put(key, val);
    }

    @Override
    public String get(String key) {
        return getCurrentContext().get(key);
    }

    @Override
    public void remove(String key) {
        getCurrentContext().remove(key);
    }

    @Override
    public void clear() {
        getCurrentContext().clear();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return Collections.unmodifiableMap(getCurrentContext().getCopyOfContextMap());
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        getCurrentContext().setContextMap(contextMap);
    }

    @Override
    public void pushByKey(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String popByKey(String key) {
        return getCurrentContext().popByKey(key);
    }

    @Override
    public Deque<String> getCopyOfDequeByKey(String key) {
        return getCurrentContext().getCopyOfDequeByKey(key);
    }

    @Override
    public void clearDequeByKey(String key) {
        getCurrentContext().clearDequeByKey(key);
    }

    private MDCAdapter getCurrentContext() {
        return SUBTASK_CONTEXT.isBound() ? SUBTASK_CONTEXT.get() : rootContext;
    }

    public void setRootContext(MDCAdapter rootContext) {
        this.rootContext = rootContext;
    }

}
