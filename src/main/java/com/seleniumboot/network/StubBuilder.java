package com.seleniumboot.network;

/**
 * Fluent builder returned by {@link NetworkMock#stub(String)}.
 *
 * <pre>
 * networkMock().stub("**&#47;api/users").returnJson("{\"users\":[]}");
 * networkMock().stub("**&#47;api/login").returnStatus(500);
 * networkMock().stub("**&#47;api/data").delay(2000);
 * </pre>
 */
public final class StubBuilder {

    private final NetworkMock owner;
    final String pattern;

    // Response configuration
    String responseBody    = "";
    String contentType     = "application/json";
    int    statusCode      = 200;
    long   delayMs         = 0;

    StubBuilder(NetworkMock owner, String pattern) {
        this.owner   = owner;
        this.pattern = pattern;
    }

    /**
     * Respond with the given JSON body (status 200, Content-Type: application/json).
     */
    public NetworkMock returnJson(String json) {
        this.responseBody = json;
        this.contentType  = "application/json";
        this.statusCode   = 200;
        owner.register(this);
        return owner;
    }

    /**
     * Respond with the given body and content type.
     */
    public NetworkMock returnBody(String body, String contentType) {
        this.responseBody = body;
        this.contentType  = contentType;
        this.statusCode   = 200;
        owner.register(this);
        return owner;
    }

    /**
     * Respond with an empty body and the given HTTP status code.
     */
    public NetworkMock returnStatus(int statusCode) {
        this.statusCode   = statusCode;
        this.responseBody = "";
        owner.register(this);
        return owner;
    }

    /**
     * Respond with the given status code and body.
     */
    public NetworkMock returnStatus(int statusCode, String body) {
        this.statusCode   = statusCode;
        this.responseBody = body;
        owner.register(this);
        return owner;
    }

    /**
     * Add an artificial delay before the response is sent (milliseconds).
     * Can be chained with a response method:
     * <pre>networkMock().stub("**&#47;slow").delay(3000).returnJson("{}");</pre>
     */
    public StubBuilder delay(long ms) {
        this.delayMs = ms;
        return this;
    }
}
