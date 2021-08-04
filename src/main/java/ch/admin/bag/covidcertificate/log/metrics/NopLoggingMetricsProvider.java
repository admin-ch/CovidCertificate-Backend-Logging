package ch.admin.bag.covidcertificate.log.metrics;

import java.time.Duration;

/**
 * Used when micrometer is not on the classpath, does not produce any metrics
 */
class NopLoggingMetricsProvider implements LoggingMetricsProvider {

    @Override
    public void incrementAsyncBufferFullFallback() {
        // nop
    }

    @Override
    public void incrementDistributedLogConnectionEstablished() {
        // nop
    }

    @Override
    public void incrementDistributedLogConnectionError() {
        // nop
    }

    @Override
    public void incrementDistributedLogTransmitError() {
        // nop
    }

    @Override
    public void incrementDistributedLogFallback() {
        // nop
    }

    @Override
    public void distributedLogTransmitTime(Duration duration) {
        // nop
    }
}
