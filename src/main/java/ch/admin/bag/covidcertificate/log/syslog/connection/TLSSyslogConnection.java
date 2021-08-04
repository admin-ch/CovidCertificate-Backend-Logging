package ch.admin.bag.covidcertificate.log.syslog.connection;

import ch.admin.bag.covidcertificate.log.metrics.LoggingMetrics;
import ch.qos.logback.core.net.ssl.SSLConfigurableSocket;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.net.ssl.SSLParametersConfiguration;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.util.CloseUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;

@RequiredArgsConstructor
public class TLSSyslogConnection {

    private SSLSocket clientSocket;

    private final String syslogHost;
    private final int port;
    private final SSLContext sslContext;
    private final SSLParametersConfiguration sslParameters;
    private final int timeoutMillis;
    private final ConnectionState connectionState = ConnectionState.disconnected();
    private boolean connectionLoggedOnce = false;
    @Getter
    private String lastTransmitError;

    public static TLSSyslogConnection create(String syslogHost, int port, SSLConfiguration ssl, ContextAware context,
                                             int timeoutMillis) throws GeneralSecurityException {
        SSLContext sslContext = ssl.createContext(context);
        SSLParametersConfiguration parameters = ssl.getParameters();
        parameters.setContext(context.getContext());
        return new TLSSyslogConnection(syslogHost, port, sslContext, parameters, timeoutMillis);
    }

    /**
     * Writes the messge to the TCP socket's write buffer, attempting to reconnect if necessary
     *
     * @return true if the message has been successfully written to the TCP socket's write buffer, false if any error occurred
     */
    public boolean transmit(byte[] syslogMessage) {
        if (connectionState.shouldReconnect()) {
            attemptConnection();
        }
        if (!connectionState.isConnected()) {
            return false;
        }

        boolean success = attemptTransmit(syslogMessage);
        // Immediate single retry on error. Monitoring has shown that most transmit errors are due to 'broken pipe'
        // errors, which means the TCP connection has been lost/reset. Connection errors do not occur however, which
        // means that transmit errors can usually be fixed by re-establishing the TCP connection.
        if (!success) {
            disconnect();
            attemptConnection();
            success = attemptTransmit(syslogMessage);
            // If the message cannot be transmitted, start exponential back off and retry re-connect later, using
            // the fallback appender in the mean time until connected to the syslog server again.
            if (!success) {
                connectionState.notifyError();
                disconnect();
            }
        }

        return success;
    }

    private boolean attemptTransmit(byte[] syslogMessage) {
        long start = System.nanoTime();
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(syslogMessage);
            outputStream.flush();
            return true;
        } catch (IOException ex) {
            lastTransmitError = ex.getMessage();
            LoggingMetrics.incrementDistributedLogTransmitError();
            return false;
        } finally {
            LoggingMetrics.distributedLogTransmitTime(Duration.ofNanos(System.nanoTime() - start));
        }
    }

    /**
     * Attempts to connect, initializes the {@link #clientSocket} connected to the syslog server and sets the connection
     * state to connected.
     * <p>
     * If the connection attempt is unsuccessful, the error is logged once per application run to stderr, and the
     * connection is left in disconnected state. No exception is thrown in this case to allow logging to the fallback
     * appender or a reconnection attempt in this case.
     */
    public void attemptConnection() {
        try {
            SSLSocket clientSocket = (SSLSocket) sslContext.getSocketFactory().createSocket();
            clientSocket.setUseClientMode(true);
            // Read timeout is used during the SSL handshake
            clientSocket.setSoTimeout(timeoutMillis);
            sslParameters.configure(new SSLConfigurableSocket(clientSocket));
            clientSocket.connect(new InetSocketAddress(syslogHost, port), timeoutMillis);
            clientSocket.startHandshake();
            clientSocket.setKeepAlive(true);
            this.clientSocket = clientSocket;
            onConnectionSuccesful();
        } catch (Exception ex) {
            disconnect();
            onConnectionError(ex);
        }
    }

    public void disconnect() {
        connectionState.notifyDisconnected();
        CloseUtil.closeQuietly(clientSocket);
        clientSocket = null;
    }

    private void onConnectionSuccesful() {
        connectionState.notifyConnected();
        LoggingMetrics.incrementDistributedLogConnectionEstablished();
        if (!connectionLoggedOnce) {
            connectionLoggedOnce = true;
            System.out.printf("TLS Syslog Appender connected to %s:%d\n", syslogHost, port);
        }
    }

    private void onConnectionError(Exception ex) {
        connectionState.notifyError();
        LoggingMetrics.incrementDistributedLogConnectionError();
        if (!connectionLoggedOnce) {
            connectionLoggedOnce = true;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(baos, true));
            System.err.printf("TLS Syslog Appender failed to connect to %s:%d: %s\n",
                    syslogHost, port, baos.toString(StandardCharsets.UTF_8));
        }
    }
}
