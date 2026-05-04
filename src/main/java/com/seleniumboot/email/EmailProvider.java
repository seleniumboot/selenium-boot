package com.seleniumboot.email;

import java.util.List;

/**
 * Backend contract for email providers (Mailhog, Mailtrap, Outlook, IMAP).
 * Implementations are selected by {@link EmailProviderFactory} based on config.
 */
public interface EmailProvider {

    /**
     * Fetches all available emails from the inbox.
     * Filtering by recipient or subject is done by {@link MailboxClient} after fetch.
     */
    List<Email> fetchAll();

    /**
     * Deletes all emails from the inbox.
     * Called when {@code email.autoClear: true} or by {@link MailboxClient#clear()}.
     */
    void clear();
}
