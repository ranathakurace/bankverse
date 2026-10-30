package com.bank.qa.core;

import com.bank.qa.api.AuthClient;
import com.bank.qa.config.RunConfig;
import com.google.gson.JsonPrimitive;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import java.util.Map;
import java.util.UUID;
import org.testng.*;
import org.testng.annotations.*;
import static org.testng.Assert.*;

public abstract class BaseTest {
	protected static final RunConfig CONFIG = new RunConfig();
	private static final ManagedApplication APPLICATION = new ManagedApplication();
	private static final ThreadLocal<TestSession> CURRENT = new ThreadLocal<>();

	@BeforeSuite(alwaysRun = true)
	public void prepareEnvironment() throws Exception {
		try {
			APPLICATION.start(CONFIG);
			try (Playwright playwright = PlaywrightFactory.create(CONFIG)) {
				var request = playwright.request().newContext(new APIRequest.NewContextOptions()
						.setBaseURL(CONFIG.apiUrl).setTimeout(CONFIG.timeout));
				try {
					assertEquals(request.get("/health").status(), 200);
					var status = request.get("/auth/bootstrap/status");
					assertEquals(status.status(), 200);
					if (Json.body(status).get("bootstrapRequired").getAsBoolean()) {
						assertTrue(CONFIG.managed,
								"Provision a dedicated ADMIN employee before targeting an existing environment");
						var created = request.post("/auth/bootstrap",
								RequestOptions.create()
										.setData(Map.of("username", CONFIG.username, "password",
												CONFIG.password, "email",
												"java-admin@example.test")));
						assertEquals(created.status(), 200,
								"Disposable administrator setup failed: " + created.text());
					}
					var session = new AuthClient(request).login(CONFIG.username, CONFIG.password);
					assertTrue(session.getAsJsonArray("roles").contains(new JsonPrimitive("ADMIN")),
							"Suite requires an ADMIN test employee");
				} finally {
					request.dispose();
				}
			}
		} catch (Exception | AssertionError e) {
			APPLICATION.close();
			throw e;
		}
	}

	@BeforeMethod(alwaysRun = true)
	public void createSession(java.lang.reflect.Method method) {
		if (!CONFIG.allowTestData && java.util.Arrays
				.asList(method.getAnnotation(org.testng.annotations.Test.class).groups())
				.contains("write"))
			throw new org.testng.SkipException("External writes require ALLOW_TEST_DATA=true");
		CURRENT.set(new TestSession(CONFIG));
	}

	protected TestSession test() {
		var session = CURRENT.get();
		if (session == null)
			throw new IllegalStateException("No test session on this thread");
		return session;
	}

	@AfterMethod(alwaysRun = true)
	public void closeSession(ITestResult result) {
		var session = CURRENT.get();
		if (session == null)
			return;
		try {
			if (!result.isSuccess())
				Reporter.log("Failure correlation ID: " + session.correlationId);
			session.evidence(!result.isSuccess(),
					result.getMethod().getMethodName() + "-" + UUID.randomUUID());
		} catch (RuntimeException e) {
			Reporter.log("Evidence capture failed: " + e.getClass().getSimpleName());
		} finally {
			try {
				session.close();
			} finally {
				CURRENT.remove();
			}
		}
	}

	@AfterSuite(alwaysRun = true)
	public void stopEnvironment() {
		APPLICATION.close();
	}
}
