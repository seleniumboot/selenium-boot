package com.seleniumboot.email;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.config.SeleniumBootConfig;
import com.seleniumboot.internal.SeleniumBootContext;

import java.util.List;

/**
 * Email inbox client — polls until a matching email arrives or the timeout expires.
 *
 * <p>Obtain via {@code BaseTest} or {@code BaseJUnit5Test}:
 * <pre>
 * // Wait for an email and assert its contents:
 * Email email = mailbox().waitForEmail(to("user@example.com"));
 * email.assertSubject("Verify your account");
 * String link = email.extractLink("Verify Email");
 * open(link);
 *
 * // Match by subject too:
 * Email email = mailbox().waitForEmail(to("user@example.com").subject("Welcome!").timeout(60));
 *
 * // Clear inbox before test:
 * mailbox().clear();
 * </pre>
 *
 * <p>Config in {@code selenium-boot.yml}:
 * <pre>
 * email:
 *   provider: mailhog   # mailhog | mailtrap | outlook | imap
 *   timeoutSeconds: 30
 *   autoClear: false
 *
 *   mailhog:
 *     host: localhost
 *     port: 8025
 *
 *   mailtrap:
 *     apiToken: ${MAILTRAP_TOKEN}
 *     accountId: ${MAILTRAP_ACCOUNT_ID}
 *     inboxId: ${MAILTRAP_INBOX_ID}
 *
 *   outlook:
 *     tenantId: ${AZURE_TENANT_ID}
 *     clientId: ${AZURE_CLIENT_ID}
 *     clientSecret: ${AZURE_CLIENT_SECRET}
 *     mailbox: test-inbox@yourcompany.com
 *
 *   imap:
 *     host: imap.gmail.com
 *     port: 993
 *     ssl: true
 *     username: ${EMAIL_USER}
 *     password: ${EMAIL_PASS}
 * </pre>
 */
@SeleniumBootApi(since = "2.0.0")
public final class MailboxClient {

    private final EmailProvider provider;
    private final SeleniumBootConfig.Email config;

    MailboxClient(EmailProvider provider, SeleniumBootConfig.Email config) {
        this.provider = provider;
        this.config   = config;
    }

    /** Creates a {@link MailboxClient} from the current suite configuration. */
    public static MailboxClient create() {
        SeleniumBootConfig.Email emailConfig = emailConfig();
        return new MailboxClient(EmailProviderFactory.create(emailConfig), emailConfig);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Polls the inbox until an email matching {@code criteria} arrives or
     * the timeout expires.
     *
     * @throws AssertionError if no matching email is found within the timeout
     */
    public Email waitForEmail(EmailCriteria criteria) {
        int timeout = criteria.getTimeoutSeconds() > 0
                ? criteria.getTimeoutSeconds()
                : config.getTimeoutSeconds();
        int pollMs  = config.getPollIntervalMs();

        long deadline = System.currentTimeMillis() + timeout * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<Email> emails = provider.fetchAll();
            for (Email email : emails) {
                if (criteria.matches(email)) return email;
            }
            try { Thread.sleep(pollMs); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError(
            "[Email] No email matching " + criteria + " received within " + timeout + "s");
    }

    /**
     * Deletes all emails from the configured inbox.
     * Call in {@code @BeforeMethod} / {@code @BeforeEach} for test isolation,
     * or enable {@code email.autoClear: true} for automatic clearing before each test.
     */
    public void clear() {
        provider.clear();
    }

    // ── Internal ──────────────────────────────────────────────────────────

    static SeleniumBootConfig.Email emailConfig() {
        try {
            SeleniumBootConfig cfg = SeleniumBootContext.getConfig();
            if (cfg != null && cfg.getEmail() != null) return cfg.getEmail();
        } catch (Exception ignored) {}
        return new SeleniumBootConfig.Email();
    }
}
