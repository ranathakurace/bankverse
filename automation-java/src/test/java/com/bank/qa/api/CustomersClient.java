package com.bank.qa.api;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class CustomersClient {
	private final APIRequestContext request;

	public CustomersClient(APIRequestContext request) {
		this.request = request;
	}

	public APIResponse register(Map<String, Object> data) {
		return request.post("/api/v1/customers/register", RequestOptions.create().setData(data));
	}

	public APIResponse get(String number) {
		return request
				.get("/api/v1/customers/" + URLEncoder.encode(number, StandardCharsets.UTF_8));
	}

	public APIResponse byEmail(String email) {
		return request.get("/api/v1/customers/search/email",
				RequestOptions.create().setQueryParam("email", email));
	}

	public APIResponse byPhone(String phone) {
		return request.get("/api/v1/customers/search/phone",
				RequestOptions.create().setQueryParam("phone", phone));
	}

	public APIResponse byPan(String pan) {
		return request.get("/api/v1/customers/search",
				RequestOptions.create().setQueryParam("pan", pan));
	}
}
