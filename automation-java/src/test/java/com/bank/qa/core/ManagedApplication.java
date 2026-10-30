package com.bank.qa.core;

import com.bank.qa.config.RunConfig;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ManagedApplication implements AutoCloseable {
	private Process process;
	private Thread hook;

	public void start(RunConfig config) throws Exception {
		if (!config.managed)
			return;
		try (var socket = new java.net.ServerSocket()) {
			socket.bind(new InetSocketAddress("127.0.0.1", config.port));
		} catch (IOException e) {
			throw new IllegalStateException(
					"Test port occupied; refusing to reuse another application", e);
		}
		Path app = config.root.getParent();
		if (!Files.exists(app.resolve("pom.xml")))
			throw new IllegalStateException("Run Maven from automation-java");
		Files.createDirectories(config.root.resolve("target"));
		boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
		var command = windows
				? List.of("cmd.exe", "/d", "/s", "/c", "mvn.cmd", "--batch-mode", "spring-boot:run",
						"-Dspring-boot.run.useTestClasspath=true")
				: List.of("mvn", "--batch-mode", "spring-boot:run",
						"-Dspring-boot.run.useTestClasspath=true");
		var builder = new ProcessBuilder(command).directory(app.toFile()).redirectErrorStream(true)
				.redirectOutput(config.root.resolve("target/application.log").toFile());
		var env = builder.environment();
		env.put("SERVER_ADDRESS", "127.0.0.1");
		env.put("SERVER_PORT", String.valueOf(config.port));
		env.put("SPRING_DATASOURCE_URL",
				"jdbc:h2:mem:bankverse_java_e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
		env.put("SPRING_DATASOURCE_USERNAME", "sa");
		env.put("SPRING_DATASOURCE_PASSWORD", "");
		env.put("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.h2.Driver");
		env.put("SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.dialect.H2Dialect");
		env.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop");
		env.put("SPRING_JPA_SHOW_SQL", "false");
		env.put("SPRING_SQL_INIT_MODE", "always");
		env.put("SPRING_SQL_INIT_SCHEMA_LOCATIONS", "file:./src/test/resources/schema.sql");
		env.put("SPRING_JPA_DEFER_DATASOURCE_INITIALIZATION", "true");
		env.put("LOGGING_LEVEL_ROOT", "WARN");
		env.put("BANKVERSE_JWT_SECRET", "java-playwright-disposable-only-signing-key-2026");
		process = builder.start();
		hook = new Thread(this::stop, "bankverse-test-shutdown");
		Runtime.getRuntime().addShutdownHook(hook);
		var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
		long deadline = System.nanoTime() + Duration.ofSeconds(180).toNanos();
		while (System.nanoTime() < deadline) {
			if (!process.isAlive())
				throw new IllegalStateException("Application exited; see target/application.log");
			try {
				var response = http.send(
						HttpRequest.newBuilder(URI.create(config.apiUrl + "/health"))
								.timeout(Duration.ofSeconds(2)).build(),
						HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() == 200 && response.body().equals("Banking API is UP"))
					return;
			} catch (IOException ignored) {
				/* Readiness polling only. */ }
			Thread.sleep(300);
		}
		throw new IllegalStateException("Readiness timed out; see target/application.log");
	}

	private synchronized void stop() {
		if (process == null)
			return;
		var descendants = new ArrayList<>(process.descendants().toList());
		Collections.reverse(descendants);
		descendants.forEach(ProcessHandle::destroy);
		process.destroy();
		try {
			process.waitFor(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
		if (process.isAlive())
			process.destroyForcibly();
		process = null;
	}

	@Override
	public void close() {
		stop();
		if (hook != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(hook);
			} catch (IllegalStateException ignored) {
			}
			hook = null;
		}
	}
}
