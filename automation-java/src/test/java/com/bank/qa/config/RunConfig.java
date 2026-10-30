package com.bank.qa.config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Properties;

public final class RunConfig {
	public final Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
	private final Properties local = new Properties();
	public final boolean managed, allowTestData, headless;
	public final String apiUrl, uiUrl, username, password, browser;
	public final int port, timeout;

	public RunConfig() {
		Path file = root.resolve("config/local.properties");
		if (Files.exists(file))
			try (var in = Files.newInputStream(file)) {
				local.load(in);
			} catch (IOException e) {
				throw new IllegalStateException("Cannot read local.properties", e);
			}
		managed = bool("startApp", "START_APP", true);
		port = integer("port", "TEST_PORT", 18080, 1, 65535);
		String localUrl = "http://127.0.0.1:" + port;
		apiUrl = origin(value("apiBaseUrl", "API_BASE_URL", localUrl));
		uiUrl = origin(value("uiBaseUrl", "UI_BASE_URL", localUrl));
		if (managed && (!apiUrl.equals(localUrl) || !uiUrl.equals(localUrl)))
			throw new IllegalArgumentException(
					"Managed mode requires local URLs matching TEST_PORT");
		username = value("username", "TEST_USERNAME", managed ? "admin" : "");
		// Managed mode creates a disposable employee through the same password policy
		// used by the application: minimum 12 characters, upper/lower case, and a digit.
		password = value("password", "TEST_PASSWORD", managed ? "Ranshu@595609" : "");
		if (username.isBlank() || password.isBlank())
			throw new IllegalArgumentException(
					"Set TEST_USERNAME and TEST_PASSWORD for external mode");
		allowTestData = managed || bool("allowTestData", "ALLOW_TEST_DATA", false);
		headless = bool("headless", "HEADLESS", true);
		browser = value("browser", "BROWSER", "chromium");
		if (!java.util.Set.of("chromium", "firefox", "webkit").contains(browser))
			throw new IllegalArgumentException("Unsupported browser");
		timeout = integer("timeoutMs", "TIMEOUT_MS", 10000, 1000, 120000);
	}

	private String value(String key, String env, String fallback) {
		return System.getProperty(key,
				System.getenv().getOrDefault(env, local.getProperty(key, fallback)));
	}

	private boolean bool(String key, String env, boolean fallback) {
		String v = value(key, env, String.valueOf(fallback));
		if (!v.equals("true") && !v.equals("false"))
			throw new IllegalArgumentException(key + " must be true or false");
		return Boolean.parseBoolean(v);
	}

	private int integer(String key, String env, int fallback, int min, int max) {
		int v = Integer.parseInt(value(key, env, String.valueOf(fallback)));
		if (v < min || v > max)
			throw new IllegalArgumentException(key + " out of range");
		return v;
	}

	private static String origin(String value) {
		URI uri = URI.create(value);
		if (uri.getScheme() == null || !java.util.Set.of("http", "https").contains(uri.getScheme())
				|| uri.getHost() == null || uri.getUserInfo() != null || uri.getPort() > 65535
				|| uri.getQuery() != null || uri.getFragment() != null
				|| !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
			throw new IllegalArgumentException(
					"Base URLs must be HTTP(S) origins without credentials or paths");
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
