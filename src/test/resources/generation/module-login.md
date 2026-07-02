# Login Module Specification

Target: Sauce Demo login page (https://www.saucedemo.com/)

## Scenarios to generate
1. **Pass**: Valid credentials redirect to inventory dashboard
2. **Edge**: Empty username shows validation error "Username is required"
3. **Fail**: Invalid password shows authentication error

## Page objects available
- `LoginPage` — enterUsername(), enterPassword(), clickLogin(), loginAs(user, pass), isErrorDisplayed(), getErrorMessage()
- `DashboardPage` — isLoaded(), getPageTitle()

## Test data
- Valid user: standard_user / secret_sauce
- Locked user: locked_out_user
- Invalid password: wrong_password
