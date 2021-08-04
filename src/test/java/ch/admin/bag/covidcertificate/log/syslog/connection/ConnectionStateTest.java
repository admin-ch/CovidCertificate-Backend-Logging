package ch.admin.bag.covidcertificate.log.syslog.connection;

import org.junit.jupiter.api.Test;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionStateTest {

    private static final int ONE_MS_ROUNDING_ERROR = 1;

    private long now = 0;

    @Test
    void connectionState_basicStateHandling() {
        ConnectionState connectionState = ConnectionState.disconnected();
        assertFalse(connectionState.isConnected());

        connectionState.notifyConnected();
        assertTrue(connectionState.isConnected());

        connectionState.notifyDisconnected();
        assertFalse(connectionState.isConnected());
    }

    @Test
    void connectionState_shouldReconnectIfDisconnected() throws InterruptedException {
        Supplier<Long> clock = () -> now;
        ConnectionState connectionState = new ConnectionState(clock);
        connectionState.notifyConnected();
        assertFalse(connectionState.shouldReconnect());

        connectionState.notifyDisconnected();
        now = 1;
        assertTrue(connectionState.shouldReconnect());
    }

    @Test
    void connectionState_exponentialBackOff() throws InterruptedException {
        Supplier<Long> clock = () -> now;
        ConnectionState connectionState = new ConnectionState(clock);
        connectionState.notifyConnected();
        assertFalse(connectionState.shouldReconnect());

        connectionState.notifyError();
        connectionState.notifyDisconnected();
        now = 100;
        assertFalse(connectionState.shouldReconnect(), "Should not reconnect after 100ms");

        now += ExponentialBackOff.DEFAULT_INITIAL_INTERVAL;
        assertTrue(connectionState.shouldReconnect(), "Should reconnected after initial interval");

        connectionState.notifyError();
        assertFalse(connectionState.shouldReconnect(), "Should not reconnect immediately after second error");

        long nowSaved = now;
        now += ExponentialBackOff.DEFAULT_INITIAL_INTERVAL;
        assertFalse(connectionState.shouldReconnect(), "Should not reconnect after single interval after second error");
        now = nowSaved;

        now += (long) (
                ExponentialBackOff.DEFAULT_INITIAL_INTERVAL * ExponentialBackOff.DEFAULT_MULTIPLIER) + ONE_MS_ROUNDING_ERROR;
        assertTrue(connectionState.shouldReconnect(), "Should reconnect after 1.5*interval after second error ");
    }
}
