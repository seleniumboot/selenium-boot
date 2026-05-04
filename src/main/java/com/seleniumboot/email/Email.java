package com.seleniumboot.email;

import com.seleniumboot.api.SeleniumBootApi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a received email. Returned by {@link MailboxClient#waitForEmail}.
 */
@SeleniumBootApi(since = "2.0.0")
public final class Email {

    private final String subject;
    private final String body;
    private final String htmlBody;
    private final String from;
    private final String to;

    public Email(String subject, String body, String htmlBody, String from, String to) {
        this.subject  = subject  != null ? subject  : "";
        this.body     = body     != null ? body     : "";
        this.htmlBody = htmlBody != null ? htmlBody : "";
        this.from     = from     != null ? from     : "";
        this.to       = to       != null ? to       : "";
    }

    public String subject()  { return subject; }
    public String body()     { return body; }
    public String htmlBody() { return htmlBody; }
    public String from()     { return from; }
    public String to()       { return to; }

    // ── Assertions ────────────────────────────────────────────────────────

    public void assertSubject(String expected) {
        if (!subject.equals(expected)) {
            throw new AssertionError(
                "[Email] Expected subject [" + expected + "] but was [" + subject + "]");
        }
    }

    public void assertBodyContains(String text) {
        if (!body.contains(text) && !htmlBody.contains(text)) {
            throw new AssertionError(
                "[Email] Expected body to contain [" + text + "] but it did not.\n" +
                "Plain body: " + body);
        }
    }

    // ── Link extraction ───────────────────────────────────────────────────

    /**
     * Finds and returns the href of the first anchor tag whose visible text equals
     * {@code linkText} (case-insensitive, leading/trailing whitespace ignored).
     *
     * <pre>
     * String verifyUrl = email.extractLink("Verify Email");
     * open(verifyUrl);
     * </pre>
     *
     * @throws AssertionError if no matching anchor is found
     */
    public String extractLink(String linkText) {
        String source = !htmlBody.isEmpty() ? htmlBody : body;
        if (source.isEmpty()) {
            throw new AssertionError("[Email] Email has no body content to extract a link from");
        }
        // Match <a ... href="url" ...>text</a> or <a ... href='url' ...>text</a>
        Pattern p = Pattern.compile(
            "<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(source);
        while (m.find()) {
            String anchorText = m.group(2).replaceAll("<[^>]+>", "").trim();
            if (anchorText.equalsIgnoreCase(linkText.trim())) {
                return m.group(1);
            }
        }
        throw new AssertionError(
            "[Email] No link with text '" + linkText + "' found in email body");
    }

    @Override
    public String toString() {
        return "Email{subject='" + subject + "', from='" + from + "', to='" + to + "'}";
    }
}
