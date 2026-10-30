package com.bank.qa.tests.ui;

import org.testng.annotations.Test;

import com.bank.qa.core.BaseTest;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginHomePageTest extends BaseTest {

	@Test(groups = { "ui" })
	public void loginShouldDisplayHomePage() {
		Page page = test().page();
		page.navigate("/");
		page.getByTestId("login-username").fill(CONFIG.username);
		page.getByTestId("login-password").fill(CONFIG.password);
		page.getByRole(AriaRole.BUTTON,
				new Page.GetByRoleOptions().setName("Sign in").setExact(true)).click();
		assertThat(page.getByTestId("session-user")).hasText(CONFIG.username);

		assertThat(page.getByText("BankVerse").visible());
		assertThat(page.getByTestId("login-form")).isHidden();
	}

}
