package com.bank.qa.tests.api;

import com.bank.qa.api.AuthClient;
import com.bank.qa.core.*;
import com.microsoft.playwright.options.RequestOptions;
import java.util.*;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static com.bank.qa.assertions.Contracts.error;

public final class RoleAccessTest extends BaseTest {
	@Test(groups = { "api", "edge", "write" })
	public void customerOfficerCannotOperateAccountsOrAdministerStaff() {
		String username = "java-officer-" + UUID.randomUUID(), password = "OfficerPlaywright2026!";
		var created = test().api().post("/api/v1/admin/employees",
				RequestOptions.create()
						.setData(Map.of("username", username, "email", username + "@example.test",
								"password", password, "roles", List.of("CUSTOMER_OFFICER"))));
		assertEquals(created.status(), 201);
		try {
			String token = new AuthClient(test().anonymous).login(username, password)
					.get("accessToken").getAsString();
			var options = RequestOptions.create().setHeader("Authorization", "Bearer " + token);
			error(test().anonymous.get("/api/v1/customers/INVALID", options), 400,
					"INVALID_CUSTOMER");
			error(test().anonymous.get("/api/v1/accounts/ACC000001", options), 403,
					"ACCESS_DENIED");
			error(test().anonymous.get("/api/v1/admin/employees", options), 403, "ACCESS_DENIED");
		} finally {
			assertEquals(
					test().api()
							.put("/api/v1/admin/employees/" + username + "/status",
									RequestOptions.create().setQueryParam("active", false))
							.status(),
					200);
		}
	}
}
