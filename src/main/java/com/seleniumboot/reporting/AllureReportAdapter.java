package com.seleniumboot.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.UUID;

/**
 * {@link ReportAdapter} that writes Allure 2-compatible JSON result files
 * to {@code target/allure-results/}, one file per test.
 *
 * <p>Enable via {@code selenium-boot.yml}:
 * <pre>
 * reporting:
 *   allure:
 *     enabled: true
 * </pre>
 *
 * <p>After the suite finishes, run the Allure CLI to generate the HTML report:
 * <pre>
 * allure serve target/allure-results
 * </pre>
 *
 * <p>Status mapping:
 * <ul>
 *   <li>PASSED  → passed</li>
 *   <li>FAILED  → failed</li>
 *   <li>SKIPPED → skipped</li>
 *   <li>other   → broken</li>
 * </ul>
 */
public class AllureReportAdapter implements ReportAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getName() {
        return "allure";
    }

    @Override
    public void generate(File metricsJson) {
        if (!metricsJson.exists()) return;

        try {
            JsonNode root  = MAPPER.readTree(metricsJson);
            JsonNode tests = root.path("tests");

            File outputDir = new File("target/allure-results");
            outputDir.mkdirs();

            // Use the metrics file's modification time as the approximate suite-end timestamp.
            // Each test's stop = suiteEnd, start = stop - totalMs.
            long suiteEnd = metricsJson.lastModified();
            if (suiteEnd == 0) suiteEnd = System.currentTimeMillis();

            for (JsonNode test : tests) {
                writeTestResult(test, outputDir, suiteEnd);
            }

            System.out.println("[Selenium Boot] Allure results   → target/allure-results/ ("
                    + tests.size() + " tests)");

        } catch (Exception e) {
            throw new RuntimeException("AllureReportAdapter failed to generate results", e);
        }
    }

    private void writeTestResult(JsonNode test, File outputDir, long suiteEnd) throws IOException {
        String testId     = test.path("testId").asText();
        String className  = test.path("testClassName").asText("");
        String status     = mapStatus(test.path("status").asText("UNKNOWN"));
        long   totalMs    = test.path("totalMs").asLong(0);
        long   stop       = suiteEnd;
        long   start      = stop - totalMs;

        ObjectNode result = MAPPER.createObjectNode();
        result.put("uuid",      UUID.randomUUID().toString());
        result.put("historyId", Integer.toHexString(testId.hashCode()));
        result.put("name",      testId);
        result.put("fullName",  className.isEmpty() ? testId : className + "#" + testId);
        result.put("status",    status);
        result.put("start",     start);
        result.put("stop",      stop);

        if (test.has("description")) {
            result.put("description", test.path("description").asText());
        }

        // Error details — only for failed/broken
        if ("failed".equals(status) || "broken".equals(status)) {
            ObjectNode details = result.putObject("statusDetails");
            details.put("message", test.path("errorMessage").asText("Test failed"));
            if (test.has("stackTrace")) {
                details.put("trace", test.path("stackTrace").asText());
            }
        }

        // Labels
        ArrayNode labels = result.putArray("labels");
        addLabel(labels, "suite",      className.isEmpty() ? "Default Suite" : className);
        addLabel(labels, "testClass",  className);
        addLabel(labels, "thread",     test.path("thread").asText());
        addLabel(labels, "framework",  "testng");
        addLabel(labels, "language",   "java");
        if (test.has("browser")) {
            addLabel(labels, "browser", test.path("browser").asText());
        }
        if (test.path("retryCount").asInt(0) > 0) {
            addLabel(labels, "flaky", "true");
        }

        // Steps
        ArrayNode stepsNode = result.putArray("steps");
        if (test.has("steps")) {
            JsonNode steps    = test.path("steps");
            int      stepCount = steps.size();
            for (int i = 0; i < stepCount; i++) {
                JsonNode step     = steps.get(i);
                long stepStart    = start + step.path("offsetMs").asLong(0);
                long stepStop     = (i + 1 < stepCount)
                        ? start + steps.get(i + 1).path("offsetMs").asLong(0)
                        : stop;

                ObjectNode s = stepsNode.addObject();
                s.put("name",   step.path("name").asText());
                s.put("status", mapStatus(step.path("status").asText("PASSED")));
                s.put("start",  stepStart);
                s.put("stop",   stepStop);
                s.putArray("parameters");

                // Step screenshot (base64 → file)
                ArrayNode stepAttachments = s.putArray("attachments");
                if (step.has("screenshotBase64")) {
                    String attachSource = saveBase64Attachment(
                            step.path("screenshotBase64").asText(), outputDir);
                    if (attachSource != null) {
                        addAttachment(stepAttachments, "Screenshot", attachSource, "image/png");
                    }
                }
            }
        }

        // Test-level screenshot attachment
        ArrayNode attachments = result.putArray("attachments");
        if (test.has("screenshotPath")) {
            File screenshot = new File(test.path("screenshotPath").asText());
            if (screenshot.exists()) {
                String attachSource = UUID.randomUUID() + "-attachment.png";
                Files.copy(screenshot.toPath(), new File(outputDir, attachSource).toPath());
                addAttachment(attachments, "Screenshot on Failure", attachSource, "image/png");
            }
        }

        result.putArray("parameters");

        MAPPER.writerWithDefaultPrettyPrinter()
              .writeValue(new File(outputDir, UUID.randomUUID() + "-result.json"), result);
    }

    // ----------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------

    private String mapStatus(String status) {
        switch (status.toUpperCase()) {
            case "PASSED":  return "passed";
            case "FAILED":  return "failed";
            case "SKIPPED": return "skipped";
            default:        return "broken";
        }
    }

    private void addLabel(ArrayNode labels, String name, String value) {
        if (value == null || value.isEmpty()) return;
        ObjectNode label = labels.addObject();
        label.put("name",  name);
        label.put("value", value);
    }

    private void addAttachment(ArrayNode attachments, String name, String source, String type) {
        ObjectNode att = attachments.addObject();
        att.put("name",   name);
        att.put("source", source);
        att.put("type",   type);
    }

    /**
     * Decodes a base64 PNG string, saves it to {@code outputDir}, and returns the file name.
     * Returns {@code null} if decoding fails.
     */
    private String saveBase64Attachment(String base64, File outputDir) {
        try {
            String data = base64.contains(",") ? base64.split(",", 2)[1] : base64;
            byte[] bytes = Base64.getDecoder().decode(data);
            String fileName = UUID.randomUUID() + "-attachment.png";
            Files.write(new File(outputDir, fileName).toPath(), bytes);
            return fileName;
        } catch (Exception e) {
            return null;
        }
    }
}
