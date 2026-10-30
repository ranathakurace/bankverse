package com.bank.qa.tests.ui;

import org.testng.annotations.Test;

import com.bank.qa.core.BaseTest;
import com.bank.qa.pages.LoginPage;

public class MyFirstUiTest extends BaseTest {

	@Test(groups = { "ui", "beginner" })
	public void employeeCanLoginWithValidCredentials() {
		var login = new LoginPage(test().page());
		login.open();
		login.login(CONFIG.username, CONFIG.password);
		login.expectSignedIn(CONFIG.username);
	}

}
