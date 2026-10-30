package com.bank.qa.assertions;

import com.bank.qa.core.Json;
import com.google.gson.JsonObject;
import com.microsoft.playwright.APIResponse;
import java.util.List;
import static org.testng.Assert.*;

public final class Contracts {
	private Contracts() {
	}

	public static JsonObject customer(APIResponse response, int status) {
		assertEquals(response.status(), status,
				"Customer HTTP status; inspect server log using correlation ID");
		assertTrue(
				response.headers().getOrDefault("content-type", "").contains("application/json"));
		var body = Json.body(response);
		assertTrue(body.get("customerNumber").getAsString().matches("CUST[0-9]{6}"));
		for (String key : List.of("fullName", "customerStatus", "kycStatus"))
			assertTrue(body.has(key) && body.get(key).isJsonPrimitive(), "Missing " + key);
		for (String key : List.of("pan", "aadhaar", "phone", "email"))
			assertFalse(body.has(key), "Sensitive field exposed: " + key);
		return body;
	}

	public static void error(APIResponse response, int status, String code) {
		assertEquals(response.status(), status, "HTTP error status");
		assertTrue(
				response.headers().getOrDefault("content-type", "").contains("application/json"));
		var body = Json.body(response);
		assertEquals(body.get("status").getAsInt(), status);
		assertEquals(body.get("errorCode").getAsString(), code);
		for (String key : List.of("message", "path", "timestamp"))
			assertTrue(body.has(key) && body.get(key).isJsonPrimitive(), "Missing " + key);
	}
}
