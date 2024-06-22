package org.example.loom.support.mdc;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LogbackServiceProvider;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.Reporter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.ServiceLoader;

public class ScopedMdcServiceProvider extends LogbackServiceProvider {

    private static final String ROOT_MDC_CONTEXT_PROPERTY_KEY = "scoped.value.root.mdc.context";

    private final ScopedMdcAdapter mdcAdapter = new ScopedMdcAdapter();

    /**
     * Adopted from {@link LogbackServiceProvider}
     */
    private final LoggerContext defaultLoggerContext = new LoggerContext();

    /**
     * Adopted from {@link LogbackServiceProvider}
     */
    private final BasicMarkerFactory markerFactory = new BasicMarkerFactory();

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return defaultLoggerContext;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    /**
     * Adopted from {@link LogbackServiceProvider}
     */
    @Override
    public void initialize() {
        defaultLoggerContext.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
        initializeLoggerContext();
        defaultLoggerContext.start();
        loadRootMdcAdapter().ifPresent( (rootMdcAdapter) -> mdcAdapter.setRootContext(rootMdcAdapter));
        // set the MDCAdapter for the defaultLoggerContext immediately
        defaultLoggerContext.setMDCAdapter(mdcAdapter);
    }

    private Optional<MDCAdapter> loadRootMdcAdapter() {
        final ClassLoader classLoader = getClass().getClassLoader();
        return loadExplicitlySpecifiedRootContext(classLoader).or( () -> {
            final SLF4JServiceProvider slf4jProvider = ServiceLoader.load(SLF4JServiceProvider.class, classLoader).stream()
                    .map((p) -> p.get())
                    .filter((sp) -> !(sp instanceof ScopedMdcServiceProvider))
                    .findFirst()
                    .orElse(null);
            if (slf4jProvider != null) {
                slf4jProvider.initialize();
                return Optional.of(slf4jProvider.getMDCAdapter());
            } else {
                return Optional.empty();
            }
        } );
    }

    /**
     * Adopted from {@link org.slf4j.LoggerFactory} bind() method
     */
    private Optional<MDCAdapter> loadExplicitlySpecifiedRootContext(ClassLoader classLoader) {
        final String explicitlySpecified = System.getProperty(ROOT_MDC_CONTEXT_PROPERTY_KEY);
        if (null == explicitlySpecified || explicitlySpecified.isEmpty()) {
            return Optional.empty();
        }
        try {
            Reporter.info(String.format("Attempting to load provider \"%s\" specified via \"%s\" system property", explicitlySpecified, ROOT_MDC_CONTEXT_PROPERTY_KEY));
            Constructor<?> constructor = classLoader.loadClass(explicitlySpecified).getConstructor();
            return Optional.of((MDCAdapter) constructor.newInstance());
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            Reporter.error(String.format("Failed to instantiate the specified SLF4JServiceProvider (%s)", explicitlySpecified), e);
            return Optional.empty();
        } catch (ClassCastException e) {
            Reporter.error(String.format("Specified SLF4JServiceProvider (%s) does not implement SLF4JServiceProvider interface", explicitlySpecified), e);
            return Optional.empty();
        }
    }

    /**
     * Adopted from {@link LogbackServiceProvider}
     */
    private void initializeLoggerContext() {
        try {
            try {
                new ContextInitializer(defaultLoggerContext).autoConfig();
            } catch (JoranException je) {
                report("Failed to auto configure default logger context", je);
            }
            // LOGBACK-292
            if (!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
                StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext);
            }
            // contextSelectorBinder.init(defaultLoggerContext, KEY);
        } catch (Exception t) { // see LOGBACK-1159
            report("Failed to instantiate [" + LoggerContext.class.getName() + "]", t);
        }
    }

    /**
     * Adopted from {@link LogbackServiceProvider}
     */
    private void report(String msg, Exception t) {
        System.err.println(msg);
        System.err.println("Reported exception:");
        t.printStackTrace();
    }

}
