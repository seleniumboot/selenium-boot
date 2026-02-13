package com.seleniumboot.test;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SampleTest extends BaseTest{

    @Test
    public void testSampleDemo() {
        getDriver().get("https://google.com");
        assertEquals(getDriver().getTitle(), "Google");
    }

    @Test
    public void testScreenshotOnFailure() {
        getDriver().get("https://google.com");
        assertEquals(getDriver().getTitle(), "Google");
    }

    @Test(enabled = false)
    public void flakyTest() {
        if (Math.random() < 0.7) {
            throw new RuntimeException("Random failure");
        }
    }

}
