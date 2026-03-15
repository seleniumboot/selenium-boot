package com.seleniumboot.steps;

public class StepRecord {

    private final String name;
    private final long   offsetMs;
    private final String status;
    private final String screenshotBase64;

    public StepRecord(String name, long offsetMs, String status, String screenshotBase64) {
        this.name              = name;
        this.offsetMs          = offsetMs;
        this.status            = status;
        this.screenshotBase64  = screenshotBase64;
    }

    public String getName()              { return name; }
    public long   getOffsetMs()          { return offsetMs; }
    public String getStatus()            { return status; }
    public String getScreenshotBase64()  { return screenshotBase64; }
}
