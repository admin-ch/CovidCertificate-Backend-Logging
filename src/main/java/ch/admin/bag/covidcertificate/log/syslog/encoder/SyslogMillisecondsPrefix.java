package ch.admin.bag.covidcertificate.log.syslog.encoder;

import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.net.SyslogConstants;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Modelled after {@link SyslogStartConverter}, but providing timestamps accurate to the millisecond. While not entirely
 * BSD syslog compliant, this format is supported by the log relay and ensures correct ordering of log entries in Splunk.
 */
class SyslogMillisecondsPrefix {

    private final DateTimeFormatter dateTimeFormatterDoubleDigitDay = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss.SSS", Locale.US);
    /**
     * <a href="https://datatracker.ietf.org/doc/html/rfc3164#page-10">rfc3164</a>:
     * "If the day of the month is less than 10, then it MUST be represented as a space and then the number."
     */
    private final DateTimeFormatter dateTimeFormatterSingleDigitDay = DateTimeFormatter.ofPattern("MMM  d HH:mm:ss.SSS", Locale.US);

    private String localHostName;

    void start() {
        localHostName = getLocalHostname();
    }

    String getPrefix(ILoggingEvent event) {
        int pri = SyslogConstants.LOG_USER + LevelToSyslogSeverity.convert(event);

        return "<" + pri + ">" +
                computeTimeStampString(event.getTimeStamp()) +
                ' ' + localHostName + ' ';
    }

    /**
     * This method gets the network name of the machine we are running on.
     * Returns "UNKNOWN_LOCALHOST" in the unlikely case where the host name
     * cannot be found.
     *
     * @return String the name of the local host
     */
    private String getLocalHostname() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException uhe) {
            return "UNKNOWN_LOCALHOST";
        }
    }

    private String computeTimeStampString(long millis) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        return localDateTime.getDayOfMonth() < 10 ?
                dateTimeFormatterSingleDigitDay.format(localDateTime) :
                dateTimeFormatterDoubleDigitDay.format(localDateTime);
    }
}
