package ch.admin.bag.covidcertificate.log.rest;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.aspectj.util.FileUtil;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
class TestHelper {
    @SneakyThrows
    void assertEqualsExceptTime(String expectFileName, String logFileName) {
        String actualLogLine = getLastLine(logFileName);
        actualLogLine = replaceTime(actualLogLine);

        URI expectFileUri = ClassLoader.getSystemResource(expectFileName).toURI();
        File expectedFile = new File(expectFileUri);
        String expectedLog = FileUtil.readAsString(expectedFile);
        expectedLog = replaceTime(expectedLog);
        expectedLog = expectedLog.replace(System.lineSeparator(), "");

        Assertions.assertEquals(expectedLog, actualLogLine);
    }

    @SneakyThrows
    private String getLastLine(String logFileName) {
        String actualFileName = "target/testoutput/" + logFileName;
        BufferedReader actualBufferedReader = Files.newBufferedReader(Path.of(actualFileName), StandardCharsets.UTF_8);
        String actual = actualBufferedReader.readLine();
        String nextLine = actualBufferedReader.readLine();
        while (nextLine != null) {
            actual = nextLine;
            nextLine = actualBufferedReader.readLine();
        }
        return actual;
    }

    private String replaceTime(String input) {
        return input
                //JSON Format 2020-05-29T12:47:17.653+02:00
                .replaceAll("\"....-..-..T..:..:......\\+..:..\"", "TIME")
                //dt
                .replaceAll("\"dt\":[0-9]+,", "\"dt\":X,")
                .replaceAll("dt=[0-9]+", "dt=X")
                //Classic Format: 2020-05-29 13:02:56,126
                .replaceAll("^....-..-.. ..:..:..,...", "TIME");
    }
}
