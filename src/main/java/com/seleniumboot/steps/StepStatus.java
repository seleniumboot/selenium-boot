package com.seleniumboot.steps;

import com.seleniumboot.api.SeleniumBootApi;

@SeleniumBootApi(since = "0.7.0")
public enum StepStatus {
    INFO, PASS, FAIL, WARN
}
