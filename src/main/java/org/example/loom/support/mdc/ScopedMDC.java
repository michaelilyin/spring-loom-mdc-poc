package org.example.loom.support.mdc;

import java.util.concurrent.Callable;

public final class ScopedMDC {
    private ScopedMDC() {
    }

    public static <T> T isolate(Callable<T> callable) throws Exception {
        return ScopedMdcAdapter.inheritCarrier(null).call(callable);
    }
}
