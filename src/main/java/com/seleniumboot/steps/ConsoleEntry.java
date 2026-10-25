package com.seleniumboot.steps;

/** A browser console error with the time it was logged, so the trace can show it under its step. */
public class ConsoleEntry {

    private final String message;
    private final long   offsetMs;

    public ConsoleEntry(String message, long offsetMs) {
        this.message  = message;
        this.offsetMs = offsetMs;
    }

    public String getMessage()  { return message; }
    public long   getOffsetMs() { return offsetMs; }
}
