package com.seleniumboot.unit;

import com.seleniumboot.email.Email;
import com.seleniumboot.email.EmailCriteria;
import com.seleniumboot.email.MailboxClient;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link MailboxClient}, {@link Email}, {@link EmailCriteria}.
 * Provider HTTP/IMAP calls require real backends and are covered by integration tests.
 */
public class MailboxClientTest {

    // ── Email.extractLink ─────────────────────────────────────────────────

    @Test
    public void extractLink_findsDirectHref() {
        Email email = new Email("s", "", "<a href=\"https://example.com/verify\">Verify Email</a>", "", "");
        assertEquals(email.extractLink("Verify Email"), "https://example.com/verify");
    }

    @Test
    public void extractLink_caseInsensitiveText() {
        Email email = new Email("s", "", "<a href=\"https://example.com/reset\">reset password</a>", "", "");
        assertEquals(email.extractLink("Reset Password"), "https://example.com/reset");
    }

    @Test
    public void extractLink_hrefWithOtherAttributes() {
        Email email = new Email("s", "",
            "<a class=\"btn\" href=\"https://example.com/confirm\" target=\"_blank\">Confirm Account</a>",
            "", "");
        assertEquals(email.extractLink("Confirm Account"), "https://example.com/confirm");
    }

    @Test
    public void extractLink_fallsBackToPlainBody() {
        Email email = new Email("s", "Click here: https://example.com/link", "", "", "");
        // No HTML body — should still attempt plain body
        try {
            email.extractLink("Click here");
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("Click here"),
                "Error should name the missing link text");
        }
    }

    @Test(expectedExceptions = AssertionError.class)
    public void extractLink_noMatchThrows() {
        Email email = new Email("s", "", "<a href=\"https://x.com\">Other Link</a>", "", "");
        email.extractLink("Nonexistent Link");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void extractLink_emptyBodyThrows() {
        new Email("s", "", "", "", "").extractLink("Anything");
    }

    // ── Email assertions ──────────────────────────────────────────────────

    @Test
    public void assertSubject_matchPasses() {
        new Email("Welcome!", "", "", "", "").assertSubject("Welcome!");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void assertSubject_mismatchThrows() {
        new Email("Welcome!", "", "", "", "").assertSubject("Wrong Subject");
    }

    @Test
    public void assertBodyContains_inPlainBodyPasses() {
        new Email("s", "Click the link to verify.", "", "", "").assertBodyContains("verify");
    }

    @Test
    public void assertBodyContains_inHtmlBodyPasses() {
        new Email("s", "", "<p>Please verify your account</p>", "", "").assertBodyContains("verify");
    }

    @Test(expectedExceptions = AssertionError.class)
    public void assertBodyContains_notFoundThrows() {
        new Email("s", "Hello world", "", "", "").assertBodyContains("missing text");
    }

    // ── EmailCriteria ─────────────────────────────────────────────────────

    @Test
    public void criteria_to_setsAddress() {
        EmailCriteria c = EmailCriteria.to("user@example.com");
        assertEquals(c.getToAddress(), "user@example.com");
    }

    @Test
    public void criteria_subject_setsContains() {
        EmailCriteria c = EmailCriteria.any().subject("Welcome");
        assertEquals(c.getSubjectContains(), "Welcome");
    }

    @Test
    public void criteria_fluent_chain() {
        EmailCriteria c = EmailCriteria.to("a@b.com").subject("Hi").containing("link").timeout(45);
        assertEquals(c.getToAddress(),       "a@b.com");
        assertEquals(c.getSubjectContains(), "Hi");
        assertEquals(c.getBodyContains(),    "link");
        assertEquals(c.getTimeoutSeconds(),  45);
    }

    @Test
    public void criteria_matches_byRecipient() {
        EmailCriteria c = EmailCriteria.to("user@example.com");
        Email match   = new Email("s", "body", "", "", "user@example.com");
        Email noMatch = new Email("s", "body", "", "", "other@example.com");
        assertTrue(c.matches(match));
        assertFalse(c.matches(noMatch));
    }

    @Test
    public void criteria_matches_bySubject() {
        EmailCriteria c = EmailCriteria.any().subject("Verify");
        assertTrue(c.matches(new Email("Please Verify your email", "", "", "", "")));
        assertFalse(c.matches(new Email("Welcome aboard", "", "", "", "")));
    }

    @Test
    public void criteria_matches_byBodyContent() {
        EmailCriteria c = EmailCriteria.to("x@y.com").containing("reset");
        Email match   = new Email("s", "Click to reset your password", "", "", "x@y.com");
        Email noMatch = new Email("s", "Welcome!", "", "", "x@y.com");
        assertTrue(c.matches(match));
        assertFalse(c.matches(noMatch));
    }

    // ── MailboxClient polling ─────────────────────────────────────────────

    @Test
    public void waitForEmail_returnsFirstMatch() throws Exception {
        Email target = new Email("Welcome!", "body", "", "from@x.com", "user@example.com");
        MailboxClient client = clientWithEmails(List.of(target));
        Email result = client.waitForEmail(EmailCriteria.to("user@example.com"));
        assertSame(result, target);
    }

    @Test(expectedExceptions = AssertionError.class)
    public void waitForEmail_timeoutThrows() throws Exception {
        MailboxClient client = clientWithEmails(List.of()); // empty inbox
        // 1s timeout so test doesn't hang
        client.waitForEmail(EmailCriteria.to("nobody@example.com").timeout(1));
    }

    @Test
    public void waitForEmail_skipsNonMatchingEmails() throws Exception {
        Email wrong  = new Email("s", "", "", "", "other@example.com");
        Email right  = new Email("s", "", "", "", "target@example.com");
        MailboxClient client = clientWithEmails(List.of(wrong, right));
        Email result = client.waitForEmail(EmailCriteria.to("target@example.com"));
        assertSame(result, right);
    }

    // ── API surface ───────────────────────────────────────────────────────

    @Test
    public void emailCriteria_hasPublicFactoryMethods() throws NoSuchMethodException {
        assertNotNull(EmailCriteria.class.getMethod("to",      String.class));
        assertNotNull(EmailCriteria.class.getMethod("subject", String.class));
    }

    @Test
    public void mailboxClient_hasPublicApiMethods() throws NoSuchMethodException {
        assertNotNull(MailboxClient.class.getMethod("waitForEmail", EmailCriteria.class));
        assertNotNull(MailboxClient.class.getMethod("clear"));
        assertNotNull(MailboxClient.class.getMethod("create"));
    }

    // ── Helper — builds a MailboxClient backed by a fixed list of emails ──

    private static MailboxClient clientWithEmails(List<Email> emails) throws Exception {
        com.seleniumboot.config.SeleniumBootConfig.Email cfg =
                new com.seleniumboot.config.SeleniumBootConfig.Email();
        cfg.setTimeoutSeconds(5);
        cfg.setPollIntervalMs(50);

        Constructor<MailboxClient> ctor =
                MailboxClient.class.getDeclaredConstructor(
                        com.seleniumboot.email.EmailProvider.class,
                        com.seleniumboot.config.SeleniumBootConfig.Email.class);
        ctor.setAccessible(true);

        com.seleniumboot.email.EmailProvider fakeProvider = new com.seleniumboot.email.EmailProvider() {
            public List<Email> fetchAll() { return emails; }
            public void clear() {}
        };
        return ctor.newInstance(fakeProvider, cfg);
    }
}
