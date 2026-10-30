package com.bank.qa.core;

import com.bank.qa.api.AuthClient;
import com.bank.qa.config.RunConfig;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import java.nio.file.*;
import java.util.*;

public final class TestSession implements AutoCloseable {
	public final RunConfig config;
	public final Playwright playwright;
	public final APIRequestContext anonymous;
	public final String correlationId = "java-pw-" + UUID.randomUUID();
	private final long owner = Thread.currentThread().getId();
	private APIRequestContext authenticated;
	private JsonObject session;
	private Browser browser;
	private BrowserContext context;
	private Page page;

	public TestSession(RunConfig config) {
		this.config = config;
		playwright = PlaywrightFactory.create(config);
		try {
			anonymous = playwright.request().newContext(new APIRequest.NewContextOptions()
					.setBaseURL(config.apiUrl).setTimeout(config.timeout));
		} catch (RuntimeException e) {
			playwright.close();
			throw e;
		}
	}

	private void checkThread() {
		if (Thread.currentThread().getId() != owner)
			throw new IllegalStateException("Playwright session must stay on its owning thread");
	}

	public JsonObject auth() {
		checkThread();
		if (session == null)
			session = new AuthClient(anonymous).login(config.username, config.password);
		return session;
	}

	public APIRequestContext api() {
		checkThread();
		if (authenticated == null)
			authenticated = playwright.request()
					.newContext(new APIRequest.NewContextOptions().setBaseURL(config.apiUrl)
							.setTimeout(config.timeout)
							.setExtraHTTPHeaders(Map.of("Authorization",
									"Bearer " + auth().get("accessToken").getAsString(),
									"X-Correlation-ID", correlationId)));
		return authenticated;
	}

	public Page page() {
		checkThread();
		if (page != null)
			return page;
		BrowserType type = switch (config.browser) {
		case "firefox" -> playwright.firefox();
		case "webkit" -> playwright.webkit();
		default -> playwright.chromium();
		};
		var options = new BrowserType.LaunchOptions().setHeadless(config.headless);
		if (config.browser.equals("chromium"))
			options.setChannel("chromium");
		browser = type.launch(options);
		context = browser.newContext(new Browser.NewContextOptions().setBaseURL(config.uiUrl)
				.setViewportSize(1440, 1000).setAcceptDownloads(true));
		context.setDefaultTimeout(config.timeout);
		context.setDefaultNavigationTimeout(20000);
		context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true)
				.setSources(true));
		page = context.newPage();
		return page;
	}

	public Page signedInPage() {
		Page result = page();
		result.navigate("/");
		var state = new LinkedHashMap<String, Object>();
		state.put("origin", config.uiUrl);
		state.put("token", auth().get("accessToken").getAsString());
		state.put("username", auth().get("username").getAsString());
		state.put("roles", auth().get("roles"));
		result.evaluate(
				"s => { if(location.origin !== s.origin) throw new Error('Unexpected UI origin'); sessionStorage.setItem('bankverse.accessToken', s.token); sessionStorage.setItem('bankverse.user', s.username); sessionStorage.setItem('bankverse.roles', JSON.stringify(s.roles)); sessionStorage.setItem('bankverse.portal', 'EMPLOYEE'); }",
				Json.GSON.fromJson(Json.GSON.toJson(state), Object.class));
		result.reload();
		com.microsoft.playwright.assertions.PlaywrightAssertions
				.assertThat(result.getByTestId("session-user")).hasText(config.username);
		return result;
	}

	public void evidence(boolean failed, String id) {
		checkThread();
		if (context == null)
			return;
		Path dir = config.root.resolve("target/artifacts").resolve(config.browser).resolve(id);
		try {
			if (failed) {
				Files.createDirectories(dir);
				if (page != null && !page.isClosed())
					page.screenshot(new Page.ScreenshotOptions().setPath(dir.resolve("failure.png"))
							.setFullPage(true));
				Files.writeString(dir.resolve("correlation.txt"), correlationId);
			}
		} catch (RuntimeException | java.io.IOException e) {
			System.err.println("Evidence capture failed: " + e.getClass().getSimpleName());
		} finally {
			var options = new Tracing.StopOptions();
			if (failed)
				options.setPath(dir.resolve("trace.zip"));
			context.tracing().stop(options);
		}
	}

	@Override
	public void close() {
		checkThread();
		try {
			if (context != null)
				context.close();
		} finally {
			try {
				if (browser != null)
					browser.close();
			} finally {
				try {
					if (authenticated != null)
						authenticated.dispose();
				} finally {
					try {
						anonymous.dispose();
					} finally {
						playwright.close();
					}
				}
			}
		}
	}
}
