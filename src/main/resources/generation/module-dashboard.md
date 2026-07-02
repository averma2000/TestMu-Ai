# Dashboard Module Specification

Target: Sauce Demo inventory page after login

## Scenarios to generate
1. **Pass**: Dashboard displays 6 products with title "Products"
2. **Edge**: Sort products low-to-high puts cheapest item first
3. **Fail**: Direct access to inventory without login redirects to login

## Page objects available
- `LoginPage` — loginAs(user, pass)
- `DashboardPage` — isLoaded(), getPageTitle(), getProductCount(), getProductNames(), sortBy(value), addFirstProductToCart(), isCartBadgeVisible()

## Notes
- Login before dashboard tests using config.validUsername() and config.validPassword()
- Sort value for low-to-high: "lohi"
