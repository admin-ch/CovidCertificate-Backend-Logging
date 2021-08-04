package ch.admin.bag.covidcertificate.rest.tracing;

public interface RestRequestListener {

    void onRequest(RestRequestTrace restRequestTrace);

    boolean isRequestListenerActive();
}
