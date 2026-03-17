package com.seleniumboot.reporting;

import com.seleniumboot.api.SeleniumBootApi;
import java.io.File;

/**
 * Extension point for custom report generation.
 *
 * <p>After each suite finishes, Selenium Boot calls {@link #generate(File)} on
 * every registered adapter. The built-in HTML adapter is always included.
 *
 * <p>Register additional adapters via Java SPI:
 * <pre>META-INF/services/com.seleniumboot.reporting.ReportAdapter</pre>
 * or programmatically:
 * <pre>ReportAdapterRegistry.register(new MySlackAdapter());</pre>
 *
 * <p>Example — posting a summary to Slack:
 * <pre>
 * public class SlackReportAdapter implements ReportAdapter {
 *     public String getName() { return "slack"; }
 *     public void generate(File metricsJson) {
 *         // parse metricsJson, build message, POST to webhook
 *     }
 * }
 * </pre>
 */
@SeleniumBootApi(since = "0.3.0")
public interface ReportAdapter {

    /** Unique human-readable name used in log messages. */
    String getName();

    /**
     * Generates a report from the supplied metrics JSON file.
     *
     * @param metricsJson the {@code target/selenium-boot-metrics.json} file
     *                    written by {@link com.seleniumboot.metrics.ExecutionMetrics}
     */
    void generate(File metricsJson);
}
