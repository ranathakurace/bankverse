package com.bank.qa.selenium.config;

import java.nio.file.*;
import java.util.Properties;

public final class RunConfig {
  private final Properties local = new Properties();
  public final Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
  public final boolean managed, headless;
  public final int port, timeoutSeconds;
  public final String uiUrl, username, password, browser;
  public RunConfig() {
    Path file = root.resolve("config/local.properties");
    if (Files.exists(file)) try (var in = Files.newInputStream(file)) { local.load(in); } catch (Exception e) { throw new IllegalStateException("Cannot read local.properties", e); }
    managed = bool("startApp", "START_APP", true); port = integer("port", "TEST_PORT", 18081);
    uiUrl = value("uiBaseUrl", "UI_BASE_URL", "http://127.0.0.1:" + port).replaceAll("/$", "");
    username = value("username", "TEST_USERNAME", "admin"); password = value("password", "TEST_PASSWORD", "SeleniumAdmin123");
    browser = value("browser", "UI_BROWSER", "chrome").toLowerCase(); headless = bool("headless", "HEADLESS", true); timeoutSeconds = integer("timeoutSeconds", "TIMEOUT_SECONDS", 15);
    if (!browser.equals("chrome") && !browser.equals("edge") && !browser.equals("firefox")) throw new IllegalArgumentException("UI_BROWSER must be chrome, edge, or firefox");
    if (!managed && (username.isBlank() || password.isBlank())) throw new IllegalArgumentException("External testing requires TEST_USERNAME and TEST_PASSWORD");
  }
  private String value(String key, String env, String fallback) { return System.getProperty(key, System.getenv().getOrDefault(env, local.getProperty(key, fallback))); }
  private boolean bool(String key, String env, boolean fallback) { String v=value(key,env,String.valueOf(fallback)); if (!v.equals("true")&&!v.equals("false")) throw new IllegalArgumentException(key+" must be true or false"); return Boolean.parseBoolean(v); }
  private int integer(String key,String env,int fallback) { int v=Integer.parseInt(value(key,env,String.valueOf(fallback))); if(v<1||v>65535) throw new IllegalArgumentException(key+" out of range"); return v; }
}
