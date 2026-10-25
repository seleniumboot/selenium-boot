package com.seleniumboot.steps;

/** A request that came back 4xx/5xx or failed outright, kept so the trace can show it under its step. */
public class NetworkFailure {

    private final String method;
    private final String url;
    private final int    status;
    private final String error;
    private final long   offsetMs;

    /** @param status HTTP status, or 0 when the request failed before any response; @param error failure text, else null */
    public NetworkFailure(String method, String url, int status, String error, long offsetMs) {
        this.method   = method;
        this.url      = url;
        this.status   = status;
        this.error    = error;
        this.offsetMs = offsetMs;
    }

    public String getMethod()  { return method; }
    public String getUrl()     { return url; }
    public int    getStatus()  { return status; }
    public String getError()   { return error; }
    public long   getOffsetMs() { return offsetMs; }
}
