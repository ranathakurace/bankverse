package com.bank.qa.tests.ui;

import com.bank.qa.api.CustomersClient;
import com.bank.qa.core.*;
import com.bank.qa.data.CustomerData;
import com.bank.qa.pages.CustomersPage;
import com.microsoft.playwright.Route;
import java.util.Map;
import org.testng.annotations.Test;
import static com.bank.qa.assertions.Contracts.customer;
import static org.testng.Assert.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class CustomersUiTest extends BaseTest {
	@Test(groups = { "ui", "smoke", "write" })
	public void uiRegistrationPersistsThroughApi() {
		var data = CustomerData.fresh();
		var screen = new CustomersPage(test().signedInPage());
		screen.open();
		String number = screen.register(data.payload());
		assertEquals(customer(new CustomersClient(test().api()).get(number), 200).get("fullName")
				.getAsString(), data.fullName());
	}

	@Test(groups = { "ui", "write" })
	public void apiCreatedCustomerFoundInUi() {
		var data = CustomerData.fresh();
		String number = customer(new CustomersClient(test().api()).register(data.payload()), 201)
				.get("customerNumber").getAsString();
		var screen = new CustomersPage(test().signedInPage());
		screen.open();
		screen.lookup(number);
		assertThat(test().page().getByTestId("api-result-body")).containsText(data.fullName());
	}

	@Test(groups = { "ui", "edge", "write" })
	public void duplicateRegistrationShowsBusinessError() {
		var data = CustomerData.fresh();
		customer(new CustomersClient(test().api()).register(data.payload()), 201);
		var screen = new CustomersPage(test().signedInPage());
		screen.open();
		screen.fill(data.payload());
		assertEquals(screen.submit().status(), 409);
		assertThat(test().page().getByTestId("banking-error-modal")).isVisible();
		assertThat(test().page().getByTestId("api-result-body"))
				.containsText("CUSTOMER_ALREADY_EXISTS");
	}

	@Test(groups = { "ui", "edge" })
	public void requiredFieldBlocksSubmission() {
		var page = test().signedInPage();
		var screen = new CustomersPage(page);
		screen.open();
		page.getByTestId("customer-submit").click();
		assertEquals(
				page.getByTestId("customer-full-name").evaluate("e => e.validity.valueMissing"),
				true);
		assertThat(page.getByTestId("api-result")).isHidden();
	}

	@Test(groups = { "ui", "edge" })
	public void serviceFailureShowsRecoverableDialog() {
		var page = test().signedInPage();
		var screen = new CustomersPage(page);
		screen.open();
		page.route("**/api/v1/customers/CUST999999",
				route -> route.fulfill(
						new Route.FulfillOptions().setStatus(503).setContentType("application/json")
								.setBody(Json.GSON.toJson(
										Map.of("status", 503, "errorCode", "SERVICE_UNAVAILABLE",
												"message", "Training service unavailable")))));
		screen.lookup("CUST999999");
		assertThat(page.getByTestId("banking-error-modal")).isVisible();
		assertThat(page.getByTestId("banking-error-modal"))
				.containsText("Training service unavailable");
		page.getByTestId("banking-error-close").click();
		assertThat(page.getByTestId("banking-error-modal")).isHidden();
	}
}
