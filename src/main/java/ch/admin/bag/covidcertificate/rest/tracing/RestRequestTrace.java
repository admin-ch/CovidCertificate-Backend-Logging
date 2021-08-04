package ch.admin.bag.covidcertificate.rest.tracing;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class RestRequestTrace {
    String method;
    String requestUri;
}
