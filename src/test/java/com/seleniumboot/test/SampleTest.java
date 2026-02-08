package com.seleniumboot.test;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SampleTest extends BaseTest{

    @Test
    public void testSampleDemo() {
        getDriver().get("https://google.com");
        assertEquals(getDriver().getTitle(), "Google");
    }
}
