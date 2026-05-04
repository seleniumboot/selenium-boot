package com.seleniumboot.email;

import com.seleniumboot.config.SeleniumBootConfig;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.RecipientStringTerm;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Email provider backed by generic IMAP (SSL or STARTTLS).
 * Works with Gmail (app passwords), Yahoo, corporate IMAP servers, and any
 * standards-compliant server.
 *
 * <p>Requires {@code com.sun.mail:jakarta.mail} on the consumer's classpath:
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.sun.mail&lt;/groupId&gt;
 *   &lt;artifactId&gt;jakarta.mail&lt;/artifactId&gt;
 *   &lt;version&gt;2.0.1&lt;/version&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <pre>
 * email:
 *   provider: imap
 *   imap:
 *     host:     imap.gmail.com
 *     port:     993
 *     ssl:      true
 *     username: ${EMAIL_USER}
 *     password: ${EMAIL_PASS}
 *     folder:   INBOX
 * </pre>
 */
final class ImapProvider implements EmailProvider {

    private final SeleniumBootConfig.Email.Imap cfg;

    ImapProvider(SeleniumBootConfig.Email.Imap cfg) {
        this.cfg = cfg;
    }

    @Override
    public List<Email> fetchAll() {
        try (Store store = openStore()) {
            Folder folder = store.getFolder(cfg.getFolder());
            folder.open(Folder.READ_ONLY);

            Message[] messages = folder.getMessages();
            List<Email> emails = new ArrayList<>();
            for (Message msg : messages) {
                emails.add(parseMessage(msg));
            }
            folder.close(false);
            return emails;
        } catch (Exception e) {
            throw new IllegalStateException("[Email/IMAP] Failed to fetch messages: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try (Store store = openStore()) {
            Folder folder = store.getFolder(cfg.getFolder());
            folder.open(Folder.READ_WRITE);
            Message[] messages = folder.getMessages();
            for (Message msg : messages) {
                msg.setFlag(Flags.Flag.DELETED, true);
            }
            folder.close(true); // expunge=true
        } catch (Exception e) {
            throw new IllegalStateException("[Email/IMAP] Failed to clear messages: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Store openStore() throws MessagingException {
        Properties props = new Properties();
        if (cfg.isSsl()) {
            props.put("mail.imap.ssl.enable", "true");
            props.put("mail.store.protocol", "imaps");
        } else {
            props.put("mail.store.protocol", "imap");
        }
        props.put("mail.imap.host", cfg.getHost());
        props.put("mail.imap.port", String.valueOf(cfg.getPort()));

        Session session = Session.getInstance(props);
        Store store = session.getStore(cfg.isSsl() ? "imaps" : "imap");
        store.connect(cfg.getHost(), cfg.getPort(),
                resolveEnv(cfg.getUsername()), resolveEnv(cfg.getPassword()));
        return store;
    }

    private static Email parseMessage(Message msg) throws Exception {
        String subject = msg.getSubject() != null ? msg.getSubject() : "";
        String from    = msg.getFrom() != null && msg.getFrom().length > 0
                ? msg.getFrom()[0].toString() : "";
        Address[] toAddrs = msg.getRecipients(Message.RecipientType.TO);
        String to = (toAddrs != null && toAddrs.length > 0) ? toAddrs[0].toString() : "";

        String plain = "";
        String html  = "";

        Object content = msg.getContent();
        if (content instanceof String) {
            String ct = msg.getContentType().toLowerCase();
            if (ct.contains("text/html"))  html  = (String) content;
            else                           plain = (String) content;
        } else if (content instanceof MimeMultipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart part = mp.getBodyPart(i);
                String ct = part.getContentType().toLowerCase();
                if (ct.startsWith("text/plain") && plain.isEmpty())
                    plain = part.getContent().toString();
                else if (ct.startsWith("text/html") && html.isEmpty())
                    html  = part.getContent().toString();
            }
        }
        return new Email(subject, plain, html, from, to);
    }

    private static String resolveEnv(String value) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String var = value.substring(2, value.length() - 1);
            String resolved = System.getenv(var);
            return resolved != null ? resolved : System.getProperty(var, value);
        }
        return value;
    }
}
