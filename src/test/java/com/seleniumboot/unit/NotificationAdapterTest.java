package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig.Notifications;
import com.seleniumboot.config.SeleniumBootConfig.Notifications.Slack;
import com.seleniumboot.config.SeleniumBootConfig.Notifications.Teams;
import com.seleniumboot.reporting.NotificationAdapter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link NotificationAdapter}.
 *
 * {@code sendWebhook()} is overridden to capture calls without making real HTTP requests.
 */
public class NotificationAdapterTest {

    private List<String[]> webhookCalls; // [url, payload]
    private File metricsFile;

    @BeforeMethod
    public void setup() throws IOException {
        webhookCalls = new ArrayList<>();
        metricsFile  = File.createTempFile("metrics", ".json");
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    /** Creates a spy adapter that records sendWebhook calls instead of making real requests. */
    private NotificationAdapter spyAdapter(Notifications config) {
        return new NotificationAdapter(config) {
            @Override
            protected int sendWebhook(String url, String payload) {
                webhookCalls.add(new String[]{url, payload});
                return 200;
            }
        };
    }

    private Notifications slackOnly(String url, boolean failureOnly) {
        Slack slack = new Slack();
        slack.setWebhookUrl(url);
        slack.setNotifyOnFailureOnly(failureOnly);
        Notifications n = new Notifications();
        n.setSlack(slack);
        return n;
    }

    private Notifications teamsOnly(String url, boolean failureOnly) {
        Teams teams = new Teams();
        teams.setWebhookUrl(url);
        teams.setNotifyOnFailureOnly(failureOnly);
        Notifications n = new Notifications();
        n.setTeams(teams);
        return n;
    }

    private Notifications both(String slackUrl, String teamsUrl) {
        Slack slack = new Slack();
        slack.setWebhookUrl(slackUrl);
        Teams teams = new Teams();
        teams.setWebhookUrl(teamsUrl);
        Notifications n = new Notifications();
        n.setSlack(slack);
        n.setTeams(teams);
        return n;
    }

    private void writeMetrics(int passed, int failed, int skipped) throws IOException {
        String failedEntries = buildFailedEntries(failed);
        String json = "{"
                + "\"totalTests\":" + (passed + failed + skipped) + ","
                + "\"passedTests\":" + passed + ","
                + "\"failedTests\":" + failed + ","
                + "\"skippedTests\":" + skipped + ","
                + "\"passRate\":"    + (passed * 100.0 / Math.max(1, passed + failed + skipped)) + ","
                + "\"flakyTests\":0,\"recoveredTests\":0,"
                + "\"totalTimeMs\":45000,"
                + "\"tests\":[" + failedEntries + "]}";
        Files.writeString(metricsFile.toPath(), json);
    }

    private String buildFailedEntries(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"testId\":\"failTest").append(i + 1).append("\","
                    + "\"testClassName\":\"MyTest\","
                    + "\"status\":\"FAILED\","
                    + "\"totalMs\":1000,"
                    + "\"thread\":\"main\","
                    + "\"retryCount\":0,"
                    + "\"errorMessage\":\"assertion failed\","
                    + "\"steps\":[]}");
        }
        return sb.toString();
    }

    // ----------------------------------------------------------
    // Slack — routing
    // ----------------------------------------------------------

    @Test
    public void slack_sendsToConfiguredWebhookUrl() throws IOException {
        writeMetrics(5, 1, 0);
        spyAdapter(slackOnly("https://hooks.slack.com/services/TEST", false))
                .generate(metricsFile);

        assertEquals(webhookCalls.size(), 1);
        assertEquals(webhookCalls.get(0)[0], "https://hooks.slack.com/services/TEST");
    }

    @Test
    public void slack_notifyOnFailureOnly_skipsWhenAllPass() throws IOException {
        writeMetrics(5, 0, 0);
        spyAdapter(slackOnly("https://hooks.slack.com/services/TEST", true))
                .generate(metricsFile);

        assertTrue(webhookCalls.isEmpty(), "No notification expected when all tests pass");
    }

    @Test
    public void slack_notifyOnFailureOnly_sendsWhenThereAreFailures() throws IOException {
        writeMetrics(4, 1, 0);
        spyAdapter(slackOnly("https://hooks.slack.com/services/TEST", true))
                .generate(metricsFile);

        assertEquals(webhookCalls.size(), 1);
    }

    // ----------------------------------------------------------
    // Teams — routing
    // ----------------------------------------------------------

    @Test
    public void teams_sendsToConfiguredWebhookUrl() throws IOException {
        writeMetrics(3, 2, 0);
        spyAdapter(teamsOnly("https://example.webhook.office.com/TEST", false))
                .generate(metricsFile);

        assertEquals(webhookCalls.size(), 1);
        assertEquals(webhookCalls.get(0)[0], "https://example.webhook.office.com/TEST");
    }

    @Test
    public void teams_notifyOnFailureOnly_skipsWhenAllPass() throws IOException {
        writeMetrics(10, 0, 0);
        spyAdapter(teamsOnly("https://example.webhook.office.com/TEST", true))
                .generate(metricsFile);

        assertTrue(webhookCalls.isEmpty());
    }

    // ----------------------------------------------------------
    // Both configured
    // ----------------------------------------------------------

    @Test
    public void bothConfigured_sendsTwoRequests() throws IOException {
        writeMetrics(2, 1, 0);
        spyAdapter(both("https://hooks.slack.com/TEST", "https://example.webhook.office.com/TEST"))
                .generate(metricsFile);

        assertEquals(webhookCalls.size(), 2);
    }

    // ----------------------------------------------------------
    // Slack payload content
    // ----------------------------------------------------------

    @Test
    public void slackPayload_containsPassRate() throws IOException {
        writeMetrics(9, 1, 0);
        spyAdapter(slackOnly("https://hooks.slack.com/TEST", false)).generate(metricsFile);

        String payload = webhookCalls.get(0)[1];
        assertTrue(payload.contains("90.0%"), "Slack payload must contain pass rate");
    }

    @Test
    public void slackPayload_containsFailedTestName() throws IOException {
        writeMetrics(0, 1, 0);
        spyAdapter(slackOnly("https://hooks.slack.com/TEST", false)).generate(metricsFile);

        String payload = webhookCalls.get(0)[1];
        assertTrue(payload.contains("failTest1"), "Slack payload must list the failed test name");
    }

    @Test
    public void slackPayload_passedSuiteShowsPassedStatus() throws IOException {
        writeMetrics(5, 0, 0);
        spyAdapter(slackOnly("https://hooks.slack.com/TEST", false)).generate(metricsFile);

        String payload = webhookCalls.get(0)[1];
        assertTrue(payload.contains("PASSED"), "Slack payload must say PASSED");
        assertFalse(payload.contains("FAILED"), "Slack payload must not say FAILED");
    }

    // ----------------------------------------------------------
    // Teams payload content
    // ----------------------------------------------------------

    @Test
    public void teamsPayload_isMessageCardFormat() throws IOException {
        writeMetrics(3, 0, 0);
        spyAdapter(teamsOnly("https://example.webhook.office.com/TEST", false))
                .generate(metricsFile);

        String payload = webhookCalls.get(0)[1];
        assertTrue(payload.contains("\"@type\""), "Teams payload must be MessageCard format");
        assertTrue(payload.contains("MessageCard"));
    }

    @Test
    public void teamsPayload_containsFailedTestName() throws IOException {
        writeMetrics(0, 2, 0);
        spyAdapter(teamsOnly("https://example.webhook.office.com/TEST", false))
                .generate(metricsFile);

        String payload = webhookCalls.get(0)[1];
        assertTrue(payload.contains("failTest1"), "Teams payload must list failed test names");
    }

    // ----------------------------------------------------------
    // Edge cases
    // ----------------------------------------------------------

    @Test
    public void generate_missingMetricsFile_isNoOp() {
        spyAdapter(slackOnly("https://hooks.slack.com/TEST", false))
                .generate(new File("target/nonexistent.json"));

        assertTrue(webhookCalls.isEmpty(), "No call expected when metrics file does not exist");
    }

    @Test
    public void getName_returnsNotification() {
        assertEquals(
                new NotificationAdapter(new Notifications()).getName(),
                "notification");
    }
}
