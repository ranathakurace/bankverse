package com.bank.qa.api;

import com.bank.qa.core.Json;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import java.util.Map;
import static org.testng.Assert.*;

public final class AuthClient {
	private final APIRequestContext request;

	public AuthClient(APIRequestContext request) {
		this.request = request;
	}

	public JsonObject login(String username, String password) {
		var response = request.post("/auth/login", RequestOptions.create()
				.setData(Map.of("username", username, "password", password)));
		assertEquals(response.status(), 200, "Employee login failed; check configured credentials");
		var session = Json.body(response);
		assertTrue(
				session.has("accessToken") && !session.get("accessToken").getAsString().isBlank(),
				"Expected access token");
		assertTrue(session.getAsJsonArray("roles").size() > 0, "Expected employee roles");
		return session;
	}
}
