package com.testmu.tests.dashboard;

import com.testmu.core.BaseUITest;
import com.testmu.pages.DashboardPage;
import com.testmu.pages.LoginPage;
import com.testmu.testdata.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;

public class DashboardTest extends BaseUITest {

    @BeforeMethod(alwaysRun = true)
    public void loginBeforeDashboardTests(Method method) {
        if (!method.getName().equals("testAccessDashboardWithoutLogin") && driver != null) {
            new LoginPage(driver).loginAs(config.validUsername(), config.validPassword());
        }
    }

    // ── Pass scenario ──────────────────────────────────────────────────

    @Test(groups = {TestGroups.DASHBOARD, TestGroups.PASS},
            description = "Dashboard should display products after successful login")
    public void testDashboardDisplaysProducts() {
        DashboardPage dashboard = new DashboardPage(driver);

        Assert.assertTrue(dashboard.isLoaded(), "Dashboard should be loaded");
        Assert.assertEquals(dashboard.getPageTitle(), "Products");
        Assert.assertEquals(dashboard.getProductCount(), 6, "Sauce Demo lists 6 products");
    }

    // ── Edge cases ─────────────────────────────────────────────────────

    @Test(groups = {TestGroups.DASHBOARD, TestGroups.EDGE},
            description = "Sorting products low to high should reorder the list")
    public void testSortProductsLowToHigh() {
        DashboardPage dashboard = new DashboardPage(driver);
        Assert.assertNotEquals(dashboard.getProductNames().get(0), "Sauce Labs Onesie",
                "Default sort should not start with cheapest item");

        dashboard.sortBy("lohi");
        List<String> sortedOrder = dashboard.getProductNames();

        Assert.assertEquals(sortedOrder.get(0), "Sauce Labs Onesie",
                "Cheapest product should appear first after low-to-high sort");
        Assert.assertEquals(sortedOrder.get(sortedOrder.size() - 1), "Sauce Labs Fleece Jacket",
                "Most expensive product should appear last after low-to-high sort");
    }

    @Test(groups = {TestGroups.DASHBOARD, TestGroups.EDGE},
            description = "Adding a product to cart should update the cart badge")
    public void testAddProductToCart() {
        DashboardPage dashboard = new DashboardPage(driver);
        Assert.assertFalse(dashboard.isCartBadgeVisible(), "Cart badge should not be visible initially");

        dashboard.addFirstProductToCart();

        Assert.assertTrue(dashboard.isCartBadgeVisible(), "Cart badge should appear after adding item");
        Assert.assertEquals(dashboard.getCartBadgeCount(), "1");
    }

    // ── Failed scenario ──────────────────────────────────────────────

    @Test(groups = {TestGroups.DASHBOARD, TestGroups.FAIL},
            description = "Direct access to inventory without login should redirect to login page")
    public void testAccessDashboardWithoutLogin() {
        driver.get(config.baseUrl() + "inventory.html");

        LoginPage loginPage = new LoginPage(driver);
        Assert.assertTrue(loginPage.isDisplayed(), "Login page should be shown for unauthenticated access");
        Assert.assertFalse(driver.getCurrentUrl().contains("inventory.html"),
                "User should not stay on inventory page without authentication");
    }
}
