package ch.admin.bag.covidcertificate.log.rest;

import ch.admin.bag.covidcertificate.rest.tracing.RestRequestTracer;
import ch.admin.bag.covidcertificate.rest.tracing.TracerConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

class RestRequestLoggerRequestTests {
    private final TracerConfiguration config = TracerConfiguration.builder()
            .attributesWhitelist(List.of("TEST"))
            .headerBlacklist(List.of())
            .uriFilterPattern(Pattern.compile(".*/actuator/.*"))
            .build();

    private final RestRequestLogger logger = new RestRequestLogger(config);
    private final RestRequestTracer tracer = new RestRequestTracer(config, List.of(logger), List.of(logger));

    @Test
    void testJsonOutput() {
        tracer.onRequestBuilder()
                .method("POST")
                .requestUri("https://example.com/test?a=b")
                .emit();
        TestHelper.assertEqualsExceptTime("json_request.log", "json.log");
    }

    @Test
    void testJsonOutput_expectActuatorRequestsToBeFiltered() {
        tracer.onRequestBuilder()
                .method("POST")
                .requestUri("https://example.com/test?a=b")
                .emit();
        tracer.onRequestBuilder()
                .method("GET")
                .requestUri("https://dev-applicationplatform-bazg-admin-ch.dev.app.cfap02.atlantica.admin.ch/applicationplatform-archrepo-service/actuator/prometheus")
                .emit();
        TestHelper.assertEqualsExceptTime("json_request.log", "json.log");
    }

    @Test
    void testClassicOutput() {
        tracer.onRequestBuilder()
                .method("POST")
                .requestUri("https://example.com/test?a=b")
                .emit();
        TestHelper.assertEqualsExceptTime("classic_request.log", "classic.log");
    }
}
