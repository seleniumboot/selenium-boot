package com.seleniumboot.pages;

import com.seleniumboot.wait.WaitEngine;
import org.openqa.selenium.By;

public class LoginPage extends BasePage {
    private final By usernameField = By.id("username");
    private final By passwordField = By.id("password");
    private final By loginButton = By.xpath("//button[contains(text(), 'Login')]");

    public void enterUsername(String username) {
        WaitEngine.waitForVisible(usernameField).sendKeys(username);
    }

    public void enterPassword(String password) {
        WaitEngine.waitForVisible(passwordField).sendKeys(password);
    }

    public void clickLoginButton() {
        WaitEngine.waitForVisible(loginButton);
    }
}
