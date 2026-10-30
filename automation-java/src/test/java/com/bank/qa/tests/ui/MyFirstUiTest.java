package com.bank.qa.tests.ui;

import org.testng.annotations.Test;

import com.bank.qa.core.BaseTest;
import com.microsoft.playwright.Page;

public class MyFirstUiTest extends BaseTest {

	@Test(groups = {"ui", "beginner"})
	public void employeeCanLoginWithValidCredentials()
	{
		Page page=test().page();
		page.navigate("http://localhost:8080/");

	}

}
