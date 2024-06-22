package org.example.loom.support.decorator;

import org.example.loom.support.mdc.ScopedMdcAdapter;
import org.example.loom.support.security.ScopedSecurityContextHolderStrategy;

/**
 * Использовать в базовых точках входа, вроде http сервера, раннера джоб, хэндлеров сообщений и т.п.
 */
public class SpawnScopedValueDecorator implements Runnable {
    private final Runnable delegate;

    public SpawnScopedValueDecorator(Runnable delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        ScopedMdcAdapter.initCarrier(
                ScopedSecurityContextHolderStrategy.initCarrier(null)
        ).run(delegate);
    }
}
