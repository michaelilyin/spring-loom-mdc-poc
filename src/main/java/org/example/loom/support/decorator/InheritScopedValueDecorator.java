package org.example.loom.support.decorator;

import org.example.loom.support.mdc.ScopedMdcAdapter;
import org.example.loom.support.security.ScopedSecurityContextHolderStrategy;

public class InheritScopedValueDecorator implements Runnable {
    private final Runnable delegate;
    private final ScopedValue.Carrier carrier;

    public InheritScopedValueDecorator(Runnable delegate) {
        this.delegate = delegate;
        this.carrier = ScopedMdcAdapter.inheritCarrier(
                ScopedSecurityContextHolderStrategy.inheritCarrier(null)
        );
    }

    @Override
    public void run() {
        carrier.run(delegate);
    }
}
