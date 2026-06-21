package com.seleniumboot.testmanagement;

/** Holds a single Xray test result to be imported at suite end. */
record XrayTestResult(String testKey, String status, String comment) {}
