package com.bank.qa.tests.ui;

import com.bank.qa.core.*;
import com.bank.qa.pages.LoginPage;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginUiTest extends BaseTest {
	@Test(groups = { "ui", "smoke" })
	public void signInAndLogoutStayEffectiveAfterReload() {
		var page = test().page();
		var login = new LoginPage(page);
		login.open();
		login.login(CONFIG.username, CONFIG.password);
		login.expectSignedIn(CONFIG.username);
		login.logout();
		assertNull(page.evaluate("sessionStorage.getItem('bankverse.accessToken')"));
		page.reload();
		assertThat(page.getByTestId("login-form")).isVisible();
	}

	@Test(groups = { "ui", "edge" })
	public void invalidCredentialsShowFeedback() {
		var page = test().page();
		var login = new LoginPage(page);
		login.open();
		login.login(CONFIG.username, "IncorrectPassword123!");
		assertThat(page.locator("#login-error")).hasText("Invalid credentials.");
		assertThat(page.getByTestId("login-form")).isVisible();
	}

	@Test(groups = { "ui", "edge" })
	public void keyboardEnterSubmitsLogin() {
		var page = test().page();
		new LoginPage(page).open();
		page.getByTestId("login-username").fill(CONFIG.username);
		page.getByTestId("login-password").fill(CONFIG.password);
		page.getByTestId("login-password").press("Enter");
		new LoginPage(page).expectSignedIn(CONFIG.username);
	}

	@Test(groups = { "ui", "edge" })
	public void authenticationSurvivesReloadButNotNewContext() {
		var page = test().signedInPage();
		page.reload();
		new LoginPage(page).expectSignedIn(CONFIG.username);
		try (var separate = page.context().browser().newContext()) {
			var other = separate.newPage();
			other.navigate(CONFIG.uiUrl);
			assertThat(other.getByTestId("login-form")).isVisible();
			assertNull(other.evaluate("sessionStorage.getItem('bankverse.accessToken')"));
		}
	}
}
