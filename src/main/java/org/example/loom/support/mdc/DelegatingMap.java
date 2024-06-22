package org.example.loom.support.mdc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelegatingMap extends ConcurrentHashMap<String, String> {
    private final Map<String, String> delegate;

    public DelegatingMap(Map<String, String> delegate) {
        super(4, 2);
        this.delegate = delegate;
    }

    @Override
    public String get(Object key) {
        var res = super.get(key);
        if (res != null) {
            return res;
        }
        return delegate.get(key);
    }
}
