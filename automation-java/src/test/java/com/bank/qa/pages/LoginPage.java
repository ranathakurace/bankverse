package com.bank.qa.pages;

import com.microsoft.playwright.Page;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginPage {
	private final Page page;

	public LoginPage(Page page) {
		this.page = page;
	}

	public void open() {
		page.navigate("/");
		assertThat(page.getByTestId("login-form")).isVisible();
	}

	public void login(String username, String password) {
		page.getByTestId("login-username").fill(username);
		page.getByTestId("login-password").fill(password);
		page.getByTestId("login-submit").click();
	}

	public void expectSignedIn(String username) {
		assertThat(page.getByTestId("session-user")).hasText(username);
	}

	public void logout() {
		page.getByTestId("logout").click();
		assertThat(page.getByTestId("login-form")).isVisible();
	}
}
