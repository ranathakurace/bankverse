package com.bank.qa.tests.api;

import com.bank.qa.api.*;
import com.bank.qa.core.*;
import com.bank.qa.data.CustomerData;
import com.bank.qa.flows.OnboardingFlow;
import com.microsoft.playwright.options.RequestOptions;
import java.math.BigDecimal;
import java.util.*;
import org.testng.annotations.*;
import static com.bank.qa.assertions.Contracts.*;
import static org.testng.Assert.*;

public final class LedgerApiTest extends BaseTest {
	private LedgerClient ledger() {
		return new LedgerClient(test().api());
	}

	private String account() {
		return new OnboardingFlow(test().api()).activeAccount();
	}

	@Test(groups = { "api", "smoke", "write" })
	public void repeatedDepositDoesNotDuplicateMoney() {
		String account = account(), key = UUID.randomUUID().toString();
		var first = ledger().deposit(key, account, "125.50");
		assertEquals(first.status(), 201);
		var again = ledger().deposit(key, account, "125.50");
		assertEquals(again.status(), 201);
		assertEquals(Json.body(again).get("reference"), Json.body(first).get("reference"));
		assertEquals(Json.body(ledger().account(account)).get("balance").getAsBigDecimal()
				.compareTo(new BigDecimal("125.50")), 0);
	}

	@Test(groups = { "api", "edge", "write" })
	public void reusedKeyReplaysOriginalAmount() {
		String account = account(), key = UUID.randomUUID().toString();
		assertEquals(ledger().deposit(key, account, "20.00").status(), 201);
		var replay = ledger().deposit(key, account, "30.00");
		assertEquals(replay.status(), 201);
		assertEquals(Json.body(replay).get("amount").getAsBigDecimal()
				.compareTo(new BigDecimal("20.00")), 0);
		assertEquals(Json.body(ledger().account(account)).get("balance").getAsBigDecimal()
				.compareTo(new BigDecimal("20.00")), 0);
	}

	@Test(groups = { "api", "edge", "write" })
	public void insufficientFundsPreserveBalance() {
		String account = account();
		error(ledger().withdraw(UUID.randomUUID().toString(), account, "1.00"), 409,
				"TRANSACTION_INVALID_STATE");
		assertEquals(Json.body(ledger().account(account)).get("balance").getAsBigDecimal()
				.compareTo(BigDecimal.ZERO), 0);
	}

	@Test(groups = { "api", "edge", "write" })
	public void idempotencyKeyCannotChangeOperation() {
		String account = account(), key = UUID.randomUUID().toString();
		assertEquals(ledger().deposit(key, account, "20.00").status(), 201);
		error(ledger().withdraw(key, account, "20.00"), 409, "TRANSACTION_INVALID_STATE");
		assertEquals(Json.body(ledger().account(account)).get("balance").getAsBigDecimal()
				.compareTo(new BigDecimal("20.00")), 0);
	}

	@DataProvider(name = "amounts")
	public Object[][] invalidAmounts() {
		return new Object[][] { { "0" }, { "-1.00" }, { "0.001" } };
	}

	@Test(groups = { "api", "edge", "write" }, dataProvider = "amounts")
	public void rejectInvalidAmounts(String amount) {
		error(ledger().deposit(UUID.randomUUID().toString(), "ACC000100", amount), 400,
				"INVALID_TRANSACTION");
	}

	@Test(groups = { "api", "edge", "write" })
	public void freezePreventsDeposit() {
		String account = account();
		var frozen = test().api().post("/api/v1/accounts/" + account + "/freeze", RequestOptions
				.create().setData(Map.of("reason", "Synthetic freeze", "performedBy", "JAVA-QA")));
		assertEquals(frozen.status(), 200);
		error(ledger().deposit(UUID.randomUUID().toString(), account, "1.00"), 409,
				"TRANSACTION_INVALID_STATE");
	}

	@Test(groups = { "api", "edge", "write" })
	public void pendingKycCannotOpenAccount() {
		String customerNumber = customer(
				new CustomersClient(test().api()).register(CustomerData.fresh().payload()), 201)
				.get("customerNumber").getAsString();
		error(test().api()
				.post("/api/v1/accounts",
						RequestOptions.create().setData(Map.of("customerNumber", customerNumber,
								"accountType", "SAVINGS", "currency", "INR"))),
				400, "INVALID_CUSTOMER");
	}
}
