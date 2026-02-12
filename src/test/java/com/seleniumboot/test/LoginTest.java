package com.seleniumboot.test;

import com.seleniumboot.pages.LoginPage;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest{

    @Test
    public void loginTest(){
        open();
        LoginPage loginPage = new LoginPage();

        loginPage.enterUsername("admin");
        loginPage.enterPassword("password");
        loginPage.clickLoginButton();
    }
}
