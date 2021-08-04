package ch.admin.bag.covidcertificate.log.metrics;

import java.time.Duration;

interface LoggingMetricsProvider {
    void incrementAsyncBufferFullFallback();

    void incrementDistributedLogConnectionEstablished();

    void incrementDistributedLogConnectionError();

    void incrementDistributedLogTransmitError();

    void incrementDistributedLogFallback();

    void distributedLogTransmitTime(Duration duration);
}
