package ch.admin.bag.covidcertificate.log.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

class MicrometerLoggingMetricsProvider implements LoggingMetricsProvider {

    private static final String ASYNC_BUFFER_FULL_METRIC = "logging_async_buffer_full_fallback";
    private static final String DIST_LOG_CONNECTION_ERROR = "logging_distlog_connection_error";
    private static final String DIST_LOG_CONNECTION_ESTABLISHED = "logging_distlog_connection_established";
    private static final String DIST_LOG_TRANSMIT_ERROR = "logging_distlog_transmit_error";
    private static final String DIST_LOG_TRANSMIT_TIME = "logging_distlog_transmit_time";
    private static final String DIST_LOG_FALLBACK = "logging_distlog_fallback";

    private final Counter asyncBufferFullFallback;
    private final Counter distLogConnectionError;
    private final Counter distLogConnectionEstablished;
    private final Counter distLogTransmitError;
    private final Counter distLogFallback;
    private final Timer logTransmitTimer;

    MicrometerLoggingMetricsProvider(Object meterRegistryBean) {
        MeterRegistry meterRegistry = (MeterRegistry) meterRegistryBean;
        // Async logging buffer full, logged to fallback logger
        asyncBufferFullFallback = Counter.builder(ASYNC_BUFFER_FULL_METRIC)
                .register(meterRegistry);
        // Failed to connect to distributed logging server
        distLogConnectionError = Counter.builder(DIST_LOG_CONNECTION_ERROR)
                .register(meterRegistry);
        // Connected to distributed log server
        distLogConnectionEstablished = Counter.builder(DIST_LOG_CONNECTION_ESTABLISHED)
                .register(meterRegistry);
        // Failed to transmit log entry to distributed logging server
        distLogTransmitError = Counter.builder(DIST_LOG_TRANSMIT_ERROR)
                .register(meterRegistry);
        // Logged to fallback logger instead of distributed logging server
        distLogFallback = Counter.builder(DIST_LOG_FALLBACK)
                .register(meterRegistry);
        logTransmitTimer = Timer.builder(DIST_LOG_TRANSMIT_TIME)
                .publishPercentiles(0.5, 0.95, 0.99)
                .distributionStatisticExpiry(Duration.ofHours(24))
                .register(meterRegistry);
    }

    @Override
    public void incrementAsyncBufferFullFallback() {
        asyncBufferFullFallback.increment();
    }

    @Override
    public void incrementDistributedLogConnectionEstablished() {
        distLogConnectionEstablished.increment();
    }

    @Override
    public void incrementDistributedLogConnectionError() {
        distLogConnectionError.increment();
    }

    @Override
    public void incrementDistributedLogTransmitError() {
        distLogTransmitError.increment();
    }

    @Override
    public void incrementDistributedLogFallback() {
        distLogFallback.increment();
    }

    @Override
    public void distributedLogTransmitTime(Duration duration) {
        logTransmitTimer.record(duration);
    }
}
