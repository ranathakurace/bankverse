package com.bank.qa.flows;

import com.bank.qa.api.CustomersClient;
import com.bank.qa.data.CustomerData;
import com.bank.qa.core.Json;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import java.util.Map;
import static com.bank.qa.assertions.Contracts.customer;
import static org.testng.Assert.*;

public final class OnboardingFlow {
	private final APIRequestContext request;

	public OnboardingFlow(APIRequestContext request) {
		this.request = request;
	}

	public String activeAccount() {
		String number = customer(
				new CustomersClient(request).register(CustomerData.fresh().payload()), 201)
				.get("customerNumber").getAsString();
		String path = "/api/v1/customers/" + number;
		post(path + "/kyc/submissions",
				Map.of("documentReference", "DOC-" + java.util.UUID.randomUUID(), "submittedBy",
						"JAVA-QA", "remarks", "Synthetic submission"),
				201);
		post(path + "/kyc/review/start",
				Map.of("reviewedBy", "JAVA-QA", "remarks", "Synthetic review"), 200);
		post(path + "/kyc/review", Map.of("decision", "APPROVE", "reviewedBy", "JAVA-QA", "remarks",
				"Synthetic approval"), 200);
		post(path + "/activate", Map.of("comments", "Synthetic test activation"), 200);
		return Json.body(post("/api/v1/accounts",
				Map.of("customerNumber", number, "accountType", "SAVINGS", "currency", "INR"), 201))
				.get("accountNumber").getAsString();
	}

	private APIResponse post(String path, Map<String, Object> payload, int status) {
		var result = request.post(path, RequestOptions.create().setData(payload));
		assertEquals(result.status(), status, "Prerequisite failed at " + path);
		return result;
	}
}
