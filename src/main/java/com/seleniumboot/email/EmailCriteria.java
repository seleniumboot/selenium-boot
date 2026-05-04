package com.seleniumboot.email;

import com.seleniumboot.api.SeleniumBootApi;

/**
 * Fluent criteria for matching an email via {@link MailboxClient#waitForEmail}.
 *
 * <pre>
 * // Wait for any email to "user@example.com":
 * mailbox().waitForEmail(to("user@example.com"));
 *
 * // Match subject and set a custom timeout:
 * mailbox().waitForEmail(to("user@example.com").subject("Welcome!").timeout(60));
 *
 * // Match body content:
 * mailbox().waitForEmail(to("user@example.com").containing("verify your account"));
 * </pre>
 */
@SeleniumBootApi(since = "2.0.0")
public final class EmailCriteria {

    private String toAddress;
    private String subjectContains;
    private String bodyContains;
    private int    timeoutSeconds = -1;  // -1 = use global config

    private EmailCriteria() {}

    // ── Static factories ──────────────────────────────────────────────────

    /** Match emails sent to the given address. */
    public static EmailCriteria to(String address) {
        EmailCriteria c = new EmailCriteria();
        c.toAddress = address;
        return c;
    }

    /**
     * Start a criteria with no initial filter — useful for subject-only or body-only searches.
     * Chain {@link #subject(String)}, {@link #containing(String)}, etc. to add conditions.
     *
     * <pre>
     * mailbox().waitForEmail(any().subject("Password Reset").timeout(30));
     * </pre>
     */
    public static EmailCriteria any() {
        return new EmailCriteria();
    }

    // ── Fluent chain ──────────────────────────────────────────────────────

    /** Add or override the recipient address filter. */
    public EmailCriteria subject(String text) {
        this.subjectContains = text;
        return this;
    }

    /** Further filter by body text (plain-text or HTML body). */
    public EmailCriteria containing(String text) {
        this.bodyContains = text;
        return this;
    }

    /** Override the global {@code email.timeoutSeconds} for this wait only. */
    public EmailCriteria timeout(int seconds) {
        this.timeoutSeconds = seconds;
        return this;
    }

    // ── Matching ──────────────────────────────────────────────────────────

    public boolean matches(Email email) {
        if (toAddress != null && !toAddress.isEmpty()
                && !email.to().toLowerCase().contains(toAddress.toLowerCase())) {
            return false;
        }
        if (subjectContains != null && !email.subject().contains(subjectContains)) {
            return false;
        }
        if (bodyContains != null
                && !email.body().contains(bodyContains)
                && !email.htmlBody().contains(bodyContains)) {
            return false;
        }
        return true;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public String getToAddress()        { return toAddress; }
    public String getSubjectContains()  { return subjectContains; }
    public String getBodyContains()     { return bodyContains; }
    public int    getTimeoutSeconds()   { return timeoutSeconds; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EmailCriteria{");
        if (toAddress       != null) sb.append("to='").append(toAddress).append("', ");
        if (subjectContains != null) sb.append("subject contains='").append(subjectContains).append("', ");
        if (bodyContains    != null) sb.append("body contains='").append(bodyContains).append("', ");
        if (timeoutSeconds  > 0)     sb.append("timeout=").append(timeoutSeconds).append("s, ");
        if (sb.charAt(sb.length() - 2) == ',') sb.setLength(sb.length() - 2);
        return sb.append('}').toString();
    }
}
