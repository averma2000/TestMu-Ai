package com.testmu.pages;

import com.testmu.agent.healing.HealingLocator;
import com.testmu.agent.healing.WaitHealingStore;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {

    private static final String PAGE = "LoginPage";
    private static final By DEFAULT_USERNAME = By.id("user-name");
    private static final By DEFAULT_PASSWORD = By.id("password");
    private static final By DEFAULT_LOGIN_BUTTON = By.id("login-button");
    private static final By DEFAULT_ERROR = By.cssSelector("[data-test='error']");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        int waitSeconds = WaitHealingStore.getInstance().getWaitSeconds(PAGE, 10);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
    }

    private By usernameField() {
        return HealingLocator.resolve(PAGE, "usernameField", DEFAULT_USERNAME);
    }

    private By passwordField() {
        return HealingLocator.resolve(PAGE, "passwordField", DEFAULT_PASSWORD);
    }

    private By loginButton() {
        return HealingLocator.resolve(PAGE, "loginButton", DEFAULT_LOGIN_BUTTON);
    }

    private By errorMessage() {
        return HealingLocator.resolve(PAGE, "errorMessage", DEFAULT_ERROR);
    }

    public LoginPage enterUsername(String username) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(usernameField()));
        field.clear();
        if (username != null) {
            field.sendKeys(username);
        }
        return this;
    }

    public LoginPage enterPassword(String password) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField()));
        field.clear();
        if (password != null) {
            field.sendKeys(password);
        }
        return this;
    }

    public void clickLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(loginButton())).click();
    }

    public DashboardPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        return new DashboardPage(driver);
    }

    public boolean isDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(loginButton())).isDisplayed();
    }

    public String getErrorMessage() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(errorMessage())).getText();
    }

    public boolean isErrorDisplayed() {
        try {
            return driver.findElement(errorMessage()).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
