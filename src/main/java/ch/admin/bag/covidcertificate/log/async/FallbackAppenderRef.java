package ch.admin.bag.covidcertificate.log.async;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import ch.qos.logback.core.spi.LifeCycle;
import lombok.Getter;

/**
 * An appender reference holder for configuring a fallback appender in the logback configuration:
 * <pre>
 *     &lt;fallback&gt;
 *         &lt;appender-ref name="my-appender"/&gt;
 *     &lt;/fallback&gt;
 * </pre>
 */
public final class FallbackAppenderRef extends AppenderAttachableImpl<ILoggingEvent> implements LifeCycle {

    @Getter
    private boolean started;

    private boolean fallbackActive;

    @Override
    public void start() {
        started = true;
        iteratorForAppenders().forEachRemaining(Appender::start);
    }

    @Override
    public void stop() {
        started = false;
        detachAndStopAllAppenders();
    }

    public void appendToFallbackAppender(ILoggingEvent loggingEvent, String reason) {
        if (!fallbackActive) {
            fallbackActive = true;
            logFallback(Level.WARN, "Activating fallback appender: " + reason);
        }
        appendLoopOnAppenders(loggingEvent);
    }

    public void notifyFallbackDeactivated() {
        if (fallbackActive) {
            fallbackActive = false;
            logFallback(Level.INFO, "Switching back from fallback to standard appender");
        }
    }

    private void logFallback(Level level, String message) {
        LoggingEvent loggingEvent = new LoggingEvent();
        loggingEvent.setLevel(level);
        loggingEvent.setLoggerName(FallbackAppenderRef.class.getName());
        loggingEvent.setMessage(message);
        loggingEvent.setTimeStamp(System.currentTimeMillis());
        loggingEvent.prepareForDeferredProcessing();
        appendLoopOnAppenders(loggingEvent);
    }
}
