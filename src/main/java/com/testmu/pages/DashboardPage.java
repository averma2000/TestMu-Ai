package com.testmu.pages;

import com.testmu.agent.healing.HealingLocator;
import com.testmu.agent.healing.WaitHealingStore;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class DashboardPage {

    private static final String PAGE = "DashboardPage";
    private static final By DEFAULT_PAGE_TITLE = By.cssSelector(".title");
    private static final By DEFAULT_INVENTORY_ITEMS = By.cssSelector(".inventory_item");
    private static final By DEFAULT_SORT_DROPDOWN = By.cssSelector("[data-test='product-sort-container']");
    private static final By DEFAULT_CART_BADGE = By.cssSelector(".shopping_cart_badge");
    private static final By DEFAULT_ADD_TO_CART = By.cssSelector("button[id^='add-to-cart']");
    private static final By DEFAULT_MENU_BUTTON = By.id("react-burger-menu-btn");
    private static final By DEFAULT_LOGOUT_LINK = By.id("logout_sidebar_link");
    private static final By DEFAULT_PRODUCT_NAME = By.cssSelector(".inventory_item_name");

    private final WebDriver driver;
    private final WebDriverWait wait;

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        int waitSeconds = WaitHealingStore.getInstance().getWaitSeconds(PAGE, 10);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
    }

    private By pageTitle() {
        return HealingLocator.resolve(PAGE, "pageTitle", DEFAULT_PAGE_TITLE);
    }

    private By inventoryItems() {
        return HealingLocator.resolve(PAGE, "inventoryItems", DEFAULT_INVENTORY_ITEMS);
    }

    private By sortDropdown() {
        return HealingLocator.resolve(PAGE, "sortDropdown", DEFAULT_SORT_DROPDOWN);
    }

    private By cartBadge() {
        return HealingLocator.resolve(PAGE, "cartBadge", DEFAULT_CART_BADGE);
    }

    private By addToCartButtons() {
        return HealingLocator.resolve(PAGE, "addToCartButtons", DEFAULT_ADD_TO_CART);
    }

    private By menuButton() {
        return HealingLocator.resolve(PAGE, "menuButton", DEFAULT_MENU_BUTTON);
    }

    private By logoutLink() {
        return HealingLocator.resolve(PAGE, "logoutLink", DEFAULT_LOGOUT_LINK);
    }

    public boolean isLoaded() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle()));
        return driver.getCurrentUrl().contains("inventory.html");
    }

    public String getPageTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(pageTitle())).getText();
    }

    public int getProductCount() {
        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(inventoryItems()));
        return driver.findElements(inventoryItems()).size();
    }

    public List<String> getProductNames() {
        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(inventoryItems()));
        return driver.findElements(DEFAULT_PRODUCT_NAME)
                .stream()
                .map(WebElement::getText)
                .toList();
    }

    public void sortBy(String optionValue) {
        WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(sortDropdown()));
        new Select(dropdown).selectByValue(optionValue);
    }

    public void addFirstProductToCart() {
        wait.until(ExpectedConditions.elementToBeClickable(addToCartButtons())).click();
    }

    public boolean isCartBadgeVisible() {
        try {
            return driver.findElement(cartBadge()).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCartBadgeCount() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(cartBadge())).getText();
    }

    public LoginPage logout() {
        wait.until(ExpectedConditions.elementToBeClickable(menuButton())).click();
        wait.until(ExpectedConditions.elementToBeClickable(logoutLink())).click();
        return new LoginPage(driver);
    }
}
