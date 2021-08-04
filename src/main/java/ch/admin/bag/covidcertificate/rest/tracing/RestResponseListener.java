package ch.admin.bag.covidcertificate.rest.tracing;

public interface RestResponseListener {

    void onResponse(RestResponseTrace restResponseTrace);

    boolean isResponseListenerActive();
}
