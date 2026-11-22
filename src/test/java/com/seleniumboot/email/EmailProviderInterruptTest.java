package com.seleniumboot.email;

import com.seleniumboot.config.SeleniumBootConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifies email providers restore the interrupt flag after interrupted HTTP calls. */
public class EmailProviderInterruptTest {

    @AfterMethod
    public void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    public void mailhogFetchAndClear_whenInterrupted_restoreInterruptFlag() {
        SeleniumBootConfig.Email.Mailhog config = new SeleniumBootConfig.Email.Mailhog();
        MailhogProvider provider = new MailhogProvider(config);

        assertInterruptedAndRestored(provider::fetchAll);
        assertInterruptedAndRestored(provider::clear);
    }

    @Test
    public void mailtrapFetchAndClear_whenInterrupted_restoreInterruptFlag() {
        SeleniumBootConfig.Email.Mailtrap config = new SeleniumBootConfig.Email.Mailtrap();
        config.setApiToken("token");
        config.setInboxId("inbox");
        MailtrapProvider provider = new MailtrapProvider(config);

        assertInterruptedAndRestored(provider::fetchAll);
        assertInterruptedAndRestored(provider::clear);
    }

    @Test
    public void outlookFetch_whenInterrupted_restoreInterruptFlag() throws ReflectiveOperationException {
        OutlookProvider provider = new OutlookProvider(outlookConfig());
        setField(provider, "accessToken", "token");
        setField(provider, "tokenExpiresAt", System.currentTimeMillis() + 120_000L);

        assertInterruptedAndRestored(provider::fetchAll);
    }

    @Test
    public void outlookClear_whenListRequestInterrupted_restoreInterruptFlag() throws Exception {
        OutlookProvider provider = new OutlookProvider(outlookConfig());
        setField(provider, "accessToken", "token");
        setField(provider, "tokenExpiresAt", System.currentTimeMillis() + 120_000L);

        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn("{\"value\":[{\"id\":\"message-id\"}]}");
        int[] requests = {0};
        doAnswer(invocation -> {
            if (requests[0]++ == 0) return response;
            Thread.interrupted();
            throw new InterruptedException();
        }).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        setField(provider, "http", http);

        assertInterruptedAndRestored(provider::clear);
    }

    @Test
    public void outlookTokenRequest_whenInterrupted_restoreInterruptFlag() {
        OutlookProvider provider = new OutlookProvider(outlookConfig());

        assertInterruptedAndRestored(provider::fetchAll);
    }

    @Test
    public void outlookDelete_whenInterrupted_restoreInterruptFlag() throws ReflectiveOperationException {
        OutlookProvider provider = new OutlookProvider(outlookConfig());
        Method deleteMessage = OutlookProvider.class.getDeclaredMethod("deleteMessage", String.class, String.class);
        deleteMessage.setAccessible(true);

        try {
            Thread.currentThread().interrupt();
            deleteMessage.invoke(provider, "message-id", "token");
            assertTrue(Thread.interrupted(), "interrupt flag should be restored after InterruptedException");
        } finally {
            Thread.interrupted();
        }
    }

    private static SeleniumBootConfig.Email.Outlook outlookConfig() {
        SeleniumBootConfig.Email.Outlook config = new SeleniumBootConfig.Email.Outlook();
        config.setTenantId("tenant");
        config.setClientId("client");
        config.setClientSecret("secret");
        config.setMailbox("mailbox@example.com");
        return config;
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void assertInterruptedAndRestored(Runnable action) {
        try {
            Thread.currentThread().interrupt();
            IllegalStateException exception = expectThrows(IllegalStateException.class, action::run);
            assertTrue(exception.getCause() instanceof InterruptedException,
                    "expected InterruptedException cause, got: " + exception.getCause());
            assertTrue(Thread.interrupted(), "interrupt flag should be restored after InterruptedException");
        } finally {
            Thread.interrupted();
        }
    }
}
