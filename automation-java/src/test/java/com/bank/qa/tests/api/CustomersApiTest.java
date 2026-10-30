package com.bank.qa.tests.api;

import com.bank.qa.api.CustomersClient;
import com.bank.qa.core.*;
import com.bank.qa.data.CustomerData;
import com.microsoft.playwright.options.RequestOptions;
import java.time.LocalDate;
import org.testng.annotations.*;
import static com.bank.qa.assertions.Contracts.*;
import static org.testng.Assert.*;

public final class CustomersApiTest extends BaseTest {
	private CustomersClient client() {
		return new CustomersClient(test().api());
	}

	@Test(groups = { "api", "smoke", "write" })
	public void registrationAndSearch() {
		var data = CustomerData.fresh();
		var created = customer(client().register(data.payload()), 201);
		String number = created.get("customerNumber").getAsString();
		assertEquals(created.get("customerStatus").getAsString(), "PENDING_KYC");
		assertEquals(created.get("kycStatus").getAsString(), "PENDING");
		for (var r : java.util.List.of(client().get(number),
				client().byEmail(" " + data.email().toUpperCase() + " "),
				client().byPan(data.pan().toLowerCase()), client().byPhone(data.phone())))
			assertEquals(customer(r, 200).get("customerNumber").getAsString(), number);
	}

	@DataProvider(name = "invalid")
	public Object[][] invalidData() {
		return new Object[][] { { "fullName", "" }, { "fullName", "A" },
				{ "fullName", "A".repeat(101) }, { "fullName", "Name123" },
				{ "email", "bad-email" }, { "email", "a".repeat(250) + "@example.test" },
				{ "phone", "123" }, { "phone", "5123456789" }, { "pan", "INVALID" },
				{ "aadhaar", "123" }, { "dob", "2099-01-01" } };
	}

	@Test(groups = { "api", "edge", "write" }, dataProvider = "invalid")
	public void invalidCustomerFields(String field, String value) {
		var data = CustomerData.fresh().payload();
		data.put(field, value);
		error(client().register(data), 400, "INVALID_CUSTOMER");
	}

	@DataProvider(name = "mandatory")
	public Object[][] mandatory() {
		return new Object[][] { { "fullName" }, { "email" }, { "phone" }, { "pan" }, { "aadhaar" },
				{ "dob" } };
	}

	@Test(groups = { "api", "edge", "write" }, dataProvider = "mandatory")
	public void omittedMandatoryField(String field) {
		var data = CustomerData.fresh().payload();
		data.remove(field);
		error(client().register(data), 400, "INVALID_CUSTOMER");
	}

	@Test(groups = { "api", "edge", "write" })
	public void eighteenthBirthdayAccepted() {
		var data = CustomerData.fresh().payload();
		data.put("dob", LocalDate.now().minusYears(18).toString());
		customer(client().register(data), 201);
	}

	@Test(groups = { "api", "edge", "write" })
	public void oneDayUnderEighteenRejected() {
		var data = CustomerData.fresh().payload();
		data.put("dob", LocalDate.now().minusYears(18).plusDays(1).toString());
		error(client().register(data), 400, "INVALID_CUSTOMER");
	}

	@Test(groups = { "api", "edge", "write" })
	public void maximumNameAccepted() {
		var data = CustomerData.fresh().payload();
		data.put("fullName", "A".repeat(100));
		customer(client().register(data), 201);
	}

	@DataProvider(name = "uniqueFields")
	public Object[][] uniqueFields() {
		return new Object[][] { { "pan" }, { "email" }, { "phone" }, { "aadhaar" } };
	}

	@Test(groups = { "api", "edge", "write" }, dataProvider = "uniqueFields")
	public void duplicateIdentityRejected(String field) {
		var first = CustomerData.fresh().payload();
		customer(client().register(first), 201);
		var second = CustomerData.fresh().payload();
		second.put(field, first.get(field));
		error(client().register(second), 409, "CUSTOMER_ALREADY_EXISTS");
	}

	@Test(groups = { "api", "edge" })
	public void invalidReference() {
		error(client().get("INVALID"), 400, "INVALID_CUSTOMER");
	}

	@Test(groups = { "api", "edge" })
	public void unknownEmail() {
		error(client().byEmail(CustomerData.fresh().email()), 404, "CUSTOMER_NOT_FOUND");
	}

	@Test(groups = { "api", "edge", "write" })
	public void malformedJson() {
		error(test().api()
				.post("/api/v1/customers/register", RequestOptions.create()
						.setHeader("Content-Type", "application/json").setData("{\"fullName\":")),
				400, "INVALID_REQUEST");
	}
}
