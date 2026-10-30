package com.bank.qa.tests.api;

import com.bank.qa.core.*;
import com.microsoft.playwright.options.RequestOptions;
import java.util.Map;
import org.testng.annotations.Test;
import static com.bank.qa.assertions.Contracts.error;
import static org.testng.Assert.*;

public final class HealthAuthTest extends BaseTest {
	@Test(groups = { "api", "smoke" })
	public void healthAndCorrelation() {
		var r = test().anonymous.get("/health",
				RequestOptions.create().setHeader("X-Correlation-ID", test().correlationId));
		assertEquals(r.status(), 200);
		assertEquals(r.text(), "Banking API is UP");
		assertEquals(r.headers().get("x-correlation-id"), test().correlationId);
	}

	@Test(groups = { "api", "smoke" })
	public void validLogin() {
		assertTrue(test().auth().getAsJsonArray("roles").toString().contains("ADMIN"));
	}

	@Test(groups = { "api", "edge" })
	public void invalidPassword() {
		error(test().anonymous.post("/auth/login",
				RequestOptions.create().setData(
						Map.of("username", CONFIG.username, "password", "IncorrectPassword123!"))),
				401, "AUTHENTICATION_FAILED");
	}

	@Test(groups = { "api", "edge" })
	public void missingAuthentication() {
		error(test().anonymous.get("/api/v1/customers/CUST000100"), 401, "AUTHENTICATION_REQUIRED");
	}

	@Test(groups = { "api", "edge" })
	public void malformedBearer() {
		error(test().anonymous.get("/api/v1/customers/CUST000100",
				RequestOptions.create().setHeader("Authorization", "Bearer malformed-token")), 401,
				"INVALID_ACCESS_TOKEN");
	}

	@Test(groups = { "api", "edge" })
	public void staleBearerDoesNotBlockLogin() {
		var r = test().anonymous.post("/auth/login",
				RequestOptions.create().setHeader("Authorization", "Bearer stale-token")
						.setData(Map.of("username", CONFIG.username, "password", CONFIG.password)));
		assertEquals(r.status(), 200);
	}

	@Test(groups = { "api", "edge" })
	public void framingPolicy() {
		assertEquals(
				test().anonymous.get("/automation-frame.html").headers().get("x-frame-options"),
				"SAMEORIGIN");
		assertEquals(test().anonymous.get("/").headers().get("x-frame-options"), "DENY");
	}
}
