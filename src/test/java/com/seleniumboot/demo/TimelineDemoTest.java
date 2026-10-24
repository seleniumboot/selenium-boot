package com.seleniumboot.demo;

import com.seleniumboot.test.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class TimelineDemoTest extends BaseTest {

  private void run(long pauseMs) throws InterruptedException {
    open();
    Thread.sleep(pauseMs); // only to give the demo varied durations
    assertTrue(getDriver().getTitle().contains("Example Domain"));
  }

  @Test public void fast1()   throws Exception { run(500); }
  @Test public void fast2()   throws Exception { run(500); }
  @Test public void medium1() throws Exception { run(2000); }
  @Test public void medium2() throws Exception { run(2500); }
  @Test public void slow1()   throws Exception { run(5000); }
  @Test public void slow2()   throws Exception { run(6000); }
  @Test public void queued1() throws Exception { run(1500); }
  @Test public void queued2() throws Exception { run(1500); }
}