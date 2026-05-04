package com.seleniumboot.email;

import com.seleniumboot.config.SeleniumBootConfig;

/**
 * Selects and constructs the correct {@link EmailProvider} from config.
 */
final class EmailProviderFactory {

    private EmailProviderFactory() {}

    static EmailProvider create(SeleniumBootConfig.Email cfg) {
        String provider = cfg.getProvider();
        if (provider == null) provider = "mailhog";

        switch (provider.toLowerCase()) {
            case "mailhog":
                return new MailhogProvider(cfg.getMailhog());

            case "mailtrap":
                return new MailtrapProvider(cfg.getMailtrap());

            case "outlook":
                return new OutlookProvider(cfg.getOutlook());

            case "imap":
                try {
                    return new ImapProvider(cfg.getImap());
                } catch (NoClassDefFoundError e) {
                    throw new IllegalStateException(
                        "[Email/IMAP] Missing dependency. Add to your pom.xml:\n" +
                        "  <dependency>\n" +
                        "    <groupId>com.sun.mail</groupId>\n" +
                        "    <artifactId>jakarta.mail</artifactId>\n" +
                        "    <version>2.0.1</version>\n" +
                        "    <scope>test</scope>\n" +
                        "  </dependency>", e);
                }

            default:
                throw new IllegalArgumentException(
                    "[Email] Unknown provider '" + provider + "'. " +
                    "Use: mailhog | mailtrap | outlook | imap");
        }
    }
}
