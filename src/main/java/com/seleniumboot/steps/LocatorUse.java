package com.seleniumboot.steps;

/** One {@code Locator} lookup made by a terminal action, kept so the trace can show it under its step. */
public class LocatorUse {

    private final String  locator;
    private final long    offsetMs;
    private final boolean found;

    public LocatorUse(String locator, long offsetMs, boolean found) {
        this.locator  = locator;
        this.offsetMs = offsetMs;
        this.found    = found;
    }

    public String  getLocator()  { return locator; }
    public long    getOffsetMs() { return offsetMs; }
    public boolean isFound()     { return found; }
}
