package com.bank.qa.api;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import java.math.BigDecimal;
import java.util.Map;

public final class LedgerClient {
	private final APIRequestContext request;

	public LedgerClient(APIRequestContext request) {
		this.request = request;
	}

	public APIResponse deposit(String key, String account, String amount) {
		return money("deposits", key, account, amount);
	}

	public APIResponse withdraw(String key, String account, String amount) {
		return money("withdrawals", key, account, amount);
	}

	private APIResponse money(String operation, String key, String account, String amount) {
		return request.post("/api/v1/transactions/" + operation,
				RequestOptions.create().setHeader("Idempotency-Key", key)
						.setData(Map.of("accountNumber", account, "amount", new BigDecimal(amount),
								"description", "Synthetic Java acceptance test")));
	}

	public APIResponse account(String account) {
		return request.get("/api/v1/accounts/" + account);
	}
}
