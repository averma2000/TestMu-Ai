package com.testmu.tests.login;

import com.testmu.core.BaseUITest;
import com.testmu.pages.DashboardPage;
import com.testmu.pages.LoginPage;
import com.testmu.testdata.TestGroups;
import com.testmu.testdata.TestUsers;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseUITest {

    // ── Pass scenario ──────────────────────────────────────────────────

    @Test(groups = {TestGroups.LOGIN, TestGroups.PASS},
            description = "Valid credentials should redirect to inventory dashboard")
    public void testValidLoginSuccess() {
        LoginPage loginPage = new LoginPage(driver);
        Assert.assertTrue(loginPage.isDisplayed(), "Login page should be displayed");

        DashboardPage dashboard = loginPage.loginAs(
                config.validUsername(),
                config.validPassword()
        );

        Assert.assertTrue(dashboard.isLoaded(), "Dashboard should load after successful login");
        Assert.assertEquals(dashboard.getPageTitle(), "Products");
        Assert.assertTrue(dashboard.getProductCount() > 0, "Products should be listed on dashboard");
    }

    // ── Edge cases ─────────────────────────────────────────────────────

    @Test(groups = {TestGroups.LOGIN, TestGroups.EDGE},
            description = "Empty username and password should show validation error")
    public void testLoginWithEmptyCredentials() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.enterUsername("");
        loginPage.enterPassword("");
        loginPage.clickLogin();

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should be displayed");
        Assert.assertEquals(loginPage.getErrorMessage(),
                "Epic sadface: Username is required");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.EDGE},
            description = "Locked out user should not be allowed to login")
    public void testLoginWithLockedOutUser() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(TestUsers.LOCKED_OUT_USER, TestUsers.VALID_PASSWORD);

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should be displayed for locked user");
        Assert.assertTrue(loginPage.getErrorMessage().contains("locked out"),
                "Error should mention account is locked out");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.EDGE},
            description = "Empty password with valid username should show validation error")
    public void testLoginWithEmptyPassword() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.enterUsername(TestUsers.STANDARD_USER);
        loginPage.enterPassword("");
        loginPage.clickLogin();

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should be displayed");
        Assert.assertEquals(loginPage.getErrorMessage(),
                "Epic sadface: Password is required");
    }

    // ── Failed scenario ──────────────────────────────────────────────

    @Test(groups = {TestGroups.LOGIN, TestGroups.FAIL},
            description = "Invalid credentials should show authentication error")
    public void testLoginWithInvalidCredentials() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(TestUsers.STANDARD_USER, TestUsers.INVALID_PASSWORD);

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should be displayed");
        Assert.assertTrue(loginPage.getErrorMessage().contains("do not match any user"),
                "Error should indicate credentials do not match");
        Assert.assertTrue(driver.getCurrentUrl().contains("saucedemo.com"),
                "User should remain on login page after failed attempt");
        Assert.assertFalse(driver.getCurrentUrl().contains("inventory"),
                "User should not reach dashboard with wrong password");
    }
}
