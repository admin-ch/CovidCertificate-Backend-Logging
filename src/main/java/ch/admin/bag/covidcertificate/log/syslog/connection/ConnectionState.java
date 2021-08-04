package ch.admin.bag.covidcertificate.log.syslog.connection;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Duration;
import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ConnectionState {
    private final static BackOff BACK_OFF;

    static {
        ExponentialBackOff exponentialBackOff = new ExponentialBackOff(ExponentialBackOff.DEFAULT_INITIAL_INTERVAL, ExponentialBackOff.DEFAULT_MULTIPLIER);
        exponentialBackOff.setMaxInterval(Duration.ofMinutes(5).toMillis());
        BACK_OFF = exponentialBackOff;
    }

    @Getter
    private boolean connected = false;

    private final Supplier<Long> clock;
    private long nextRetryMillis;
    private BackOffExecution backOffExecution;

    static ConnectionState disconnected() {
        return new ConnectionState(System::currentTimeMillis);
    }

    void notifyConnected() {
        connected = true;
        backOffExecution = null;
        nextRetryMillis = 0;
    }

    void notifyDisconnected() {
        connected = false;
    }

    void notifyError() {
        if (backOffExecution == null) {
            backOffExecution = BACK_OFF.start();
        }
        nextRetryMillis = clock.get() + backOffExecution.nextBackOff();
    }

    boolean shouldReconnect() {
        if (connected) {
            return false;
        }
        return clock.get() > nextRetryMillis;
    }

}
