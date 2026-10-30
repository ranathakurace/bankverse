package com.bank.qa.pages;

import com.bank.qa.core.Json;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.util.Map;
import static org.testng.Assert.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class CustomersPage {
	private final Page page;

	public CustomersPage(Page page) {
		this.page = page;
	}

	public void open() {
		page.getByTestId("nav-customers").click();
		assertThat(page.getByTestId("customer-registration-form")).isVisible();
	}

	public void fill(Map<String, Object> data) {
		Map<String, String> ids = Map.of("fullName", "customer-full-name", "email", "customer-email", "phone",
				"customer-phone", "pan", "customer-pan", "aadhaar", "customer-aadhaar", "dob", "customer-dob");
		ids.forEach((key, id) -> page.getByTestId(id).fill(String.valueOf(data.get(key))));
	}

	public Response submit() {
		return page.waitForResponse(
				r -> r.url().endsWith("/api/v1/customers/register") && r.request().method().equals("POST"),
				() -> page.getByTestId("customer-submit").click());
	}

	public String register(Map<String, Object> data) {
		fill(data);
		var response = submit();
		assertEquals(response.status(), 201);
		String number = com.google.gson.JsonParser.parseString(response.text()).getAsJsonObject().get("customerNumber")
				.getAsString();
		assertThat(page.getByTestId("api-result-body")).containsText(number);
		return number;
	}

	public void lookup(String number) {
		var form = page.locator("#customer-lookup-form");
		form.getByLabel("Customer number").fill(number);
		form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Retrieve record")).click();
	}
}
