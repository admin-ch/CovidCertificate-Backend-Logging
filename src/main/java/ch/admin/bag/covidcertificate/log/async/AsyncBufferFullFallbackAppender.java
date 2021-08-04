package ch.admin.bag.covidcertificate.log.async;

import ch.admin.bag.covidcertificate.log.metrics.LoggingMetrics;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AsyncAppenderBase;
import lombok.Setter;

/**
 * Async appender with a reference to a fallback appender to use if the async logging event buffer is nearing capacity.
 * Compared to the {@link ch.qos.logback.classic.AsyncAppender} from logback, the {@link AsyncBufferFullFallbackAppender}
 * will spill over log events to the fallback instead of blocking or discarding log events in case the buffer is nearly full.
 * <p>
 * The fallback is used if less than fallbackThreshold (default: 20%) of the buffer size is available.
 * <p>
 * Typical use case: Spill over log events to a faster appender if the appender logged to by the async appender cannot
 * keep up with the amount of logged events.
 * <p>
 * The fallback appender can be disabled using {@link #setUseFallbackAppender(boolean)}, which is useful to allow for
 * configuratively disabling the fallback mechanism via a property. In this case the appender behaves just like the
 * {@link ch.qos.logback.classic.AsyncAppender} from logback.
 */
public class AsyncBufferFullFallbackAppender extends AsyncAppenderBase<ILoggingEvent> {

    private static final int UNDEFINED = -1;

    @Setter
    private boolean useFallbackAppender = true;

    @Setter
    private int fallbackThreshold = UNDEFINED;

    @Setter
    private FallbackAppenderRef fallback;

    public AsyncBufferFullFallbackAppender() {
        // This appender uses a fallback instead of discarding messages silently,
        // the discarding threshold is thus defaulted to 0 to avoid discarding any messages
        setDiscardingThreshold(0);
    }

    /**
     * Append a log event asynchronously, falling back to the fallback appender if
     * <code>getRemainingCapacity() <= fallbackThreshold</code>
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (useFallbackAppender && isBufferNearlyFull()) {
            LoggingMetrics.incrementAsyncBufferFullFallback();
            fallback.appendToFallbackAppender(eventObject, "Async buffer full");
        } else {
            fallback.notifyFallbackDeactivated();
            super.append(eventObject);
        }
    }

    private boolean isBufferNearlyFull() {
        return getRemainingCapacity() <= fallbackThreshold;
    }

    @Override
    protected void preprocess(ILoggingEvent eventObject) {
        eventObject.prepareForDeferredProcessing();
    }

    @Override
    public void start() {
        if (fallbackThreshold == UNDEFINED) {
            fallbackThreshold = getQueueSize() / 5;
        }
        fallback.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        fallback.stop();
    }
}
