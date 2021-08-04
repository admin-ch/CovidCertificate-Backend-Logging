package ch.admin.bag.covidcertificate.log.syslog;

import ch.admin.bag.covidcertificate.log.syslog.connection.TLSSyslogConnection;
import ch.admin.bag.covidcertificate.log.async.FallbackAppenderRef;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.spi.ContextAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TLSSyslogAppenderTest {

    private static final String LOGGED_MESSAGE = "logged message";

    @Mock
    private Appender<ILoggingEvent> fallbackAppender;
    @Mock
    private TLSSyslogConnection tlsSyslogConnection;
    @Mock
    private ILoggingEvent loggingEvent;
    private final List<String> transmittedMessages = new ArrayList<>();

    private TLSSyslogAppender tlsSyslogAppender;

    @Test
    void append_when_syslogTransmitIsSuccesful_then_shouldTransmitMessage() {
        stubSuccesfulSyslogConnectionTransmit();

        tlsSyslogAppender.append(loggingEvent);

        verify(tlsSyslogConnection).attemptConnection();
        assertEquals(1, transmittedMessages.size());
        assertEquals(LOGGED_MESSAGE, transmittedMessages.get(0));
    }

    @Test
    void append_when_messageExceedsMaxSize_then_shouldTruncateMessage() {
        tlsSyslogAppender.setMaxMessageSize(3);
        stubSuccesfulSyslogConnectionTransmit();

        tlsSyslogAppender.append(loggingEvent);

        assertEquals(1, transmittedMessages.size());
        assertEquals(LOGGED_MESSAGE.substring(0, 3), transmittedMessages.get(0));
    }

    @Test
    void append_when_syslogTransmitFails_then_shouldSubmitLogEventToFallbackAppender() {
        when(tlsSyslogConnection.transmit(any(byte[].class)))
                .thenReturn(false);

        tlsSyslogAppender.append(loggingEvent);

        verify(fallbackAppender).doAppend(loggingEvent);
    }

    private void stubSuccesfulSyslogConnectionTransmit() {
        when(tlsSyslogConnection.transmit(any(byte[].class)))
                .thenAnswer(invocation ->
                        transmittedMessages.add(new String(invocation.getArgument(0), StandardCharsets.UTF_8)));
    }

    @BeforeEach
    void beforeEach() {
        Encoder<ILoggingEvent> encoder = new EncoderStub();
        FallbackAppenderRef fallback = new FallbackAppenderRef();
        fallback.addAppender(fallbackAppender);

        tlsSyslogAppender = new TLSSyslogAppender() {
            @Override
            protected TLSSyslogConnection createSyslogConnection(ContextAware contextAware) {
                return tlsSyslogConnection;
            }
        };
        tlsSyslogAppender.setSyslogHost("host");
        tlsSyslogAppender.setPort(1234);
        tlsSyslogAppender.setEncoder(encoder);
        tlsSyslogAppender.setFallback(fallback);
        tlsSyslogAppender.start();
        when(loggingEvent.getFormattedMessage()).thenReturn(LOGGED_MESSAGE);
    }

    private static class EncoderStub extends EncoderBase<ILoggingEvent> {
        @Override
        public byte[] encode(ILoggingEvent event) {
            return event.getFormattedMessage().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public byte[] headerBytes() {
            return null;
        }

        @Override
        public byte[] footerBytes() {
            return null;
        }
    }
}
