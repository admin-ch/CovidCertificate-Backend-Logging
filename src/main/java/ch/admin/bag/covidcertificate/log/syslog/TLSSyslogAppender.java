package ch.admin.bag.covidcertificate.log.syslog;

import ch.admin.bag.covidcertificate.log.async.FallbackAppenderRef;
import ch.admin.bag.covidcertificate.log.metrics.LoggingMetrics;
import ch.admin.bag.covidcertificate.log.syslog.connection.TLSSyslogConnection;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.ssl.SSLComponent;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.spi.ContextAware;
import lombok.Getter;
import lombok.Setter;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * A logback appender sending messages using a {@link TLSSyslogConnection} via TCP/SSL to a syslog server. The
 * {@link #setEncoder(Encoder) encoder} is responsible for providing the message payload understood by the receiving
 * syslog server. When the syslog message transfer is observed to fail, a {@link #setFallback(FallbackAppenderRef) fallback}
 * appender is used to append messages to the log (i.e. a console or file fallback).
 */
public class TLSSyslogAppender extends AppenderBase<ILoggingEvent> implements SSLComponent {

    private static final int DEFAULT_PORT = 6514;
    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;
    private static final int MAX_MESSAGE_SIZE_LIMIT = 65000;

    @Setter
    private String syslogHost;
    @Setter
    private int port = DEFAULT_PORT;
    @Setter
    private int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
    @Setter
    private int maxMessageSize = MAX_MESSAGE_SIZE_LIMIT;
    @Setter
    private Encoder<ILoggingEvent> encoder;
    @Setter
    private FallbackAppenderRef fallback;
    @Setter
    @Getter
    private SSLConfiguration ssl = new SSLConfiguration();

    private TLSSyslogConnection syslogConnection;

    @Override
    protected void append(ILoggingEvent eventObject) {
        byte[] syslogMessage = encoder.encode(eventObject);

        if (!transmit(syslogMessage)) {
            LoggingMetrics.incrementDistributedLogFallback();
            fallback.appendToFallbackAppender(eventObject, "TLSSyslogAppender: " + syslogConnection.getLastTransmitError());
        } else {
            fallback.notifyFallbackDeactivated();
        }
    }

    protected boolean transmit(byte[] syslogMessage) {
        byte[] maxLengthSyslogMessage = syslogMessage;
        if (syslogMessage.length > maxMessageSize) {
            maxLengthSyslogMessage = Arrays.copyOfRange(syslogMessage, 0, maxMessageSize);
        }
        return syslogConnection.transmit(maxLengthSyslogMessage);
    }

    @Override
    public void start() {
        if (syslogHost == null) {
            throw new IllegalArgumentException("syslogHost must be configured for " + getClass().getSimpleName());
        }
        if (fallback == null) {
            throw new IllegalArgumentException("fallback must be configured for " + getClass().getSimpleName());
        }
        if (encoder == null) {
            throw new IllegalArgumentException("an encoder must be configured for " + getClass().getSimpleName());
        }

        try {
            ContextAware contextAware = this;
            syslogConnection = createSyslogConnection(contextAware);
            super.start();

            // This will not fail if unable to connect - avoids blocking the application from starting if the syslog
            // host is not available.
            syslogConnection.attemptConnection();
        } catch (Exception e) {
            addError(format("Error starting " + getClass().getSimpleName() + " using syslog host %s:%d", syslogHost, port), e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (syslogConnection != null) {
            syslogConnection.disconnect();
            syslogConnection = null;
        }
    }

    protected TLSSyslogConnection createSyslogConnection(ContextAware contextAware) throws GeneralSecurityException {
        return TLSSyslogConnection.create(syslogHost, port, getSsl(), contextAware, timeoutMillis);
    }
}
