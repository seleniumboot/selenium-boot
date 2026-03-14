package com.seleniumboot.reporting;

import java.io.File;

/**
 * Built-in {@link ReportAdapter} that delegates to {@link HtmlReportGenerator}.
 * Always registered by {@link ReportAdapterRegistry} — users do not need to add it.
 */
public final class HtmlReportAdapter implements ReportAdapter {

    @Override
    public String getName() {
        return "html";
    }

    @Override
    public void generate(File metricsJson) {
        HtmlReportGenerator.generate();
    }
}
