package com.bank.qa.selenium.core;

import com.bank.qa.selenium.config.RunConfig;
import com.bank.qa.selenium.pages.LoginPage;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

public abstract class BaseUiTest {
  protected static final RunConfig CONFIG=new RunConfig();
  private static final ManagedApplication APP=new ManagedApplication();
  protected WebDriver driver; protected WebDriverWait wait;
  private long ownerThread;
  private static final Set<String> ACTIVE_SESSIONS = ConcurrentHashMap.newKeySet();
  private static final ConcurrentLinkedQueue<String> SESSION_EVENTS = new ConcurrentLinkedQueue<>();
  private String session;
  @BeforeSuite(alwaysRun=true) public void startApplication() throws Exception { APP.start(CONFIG); bootstrapAdminIfRequired(); }
  private void bootstrapAdminIfRequired() throws Exception {
    if(!CONFIG.managed) return;
    var client=HttpClient.newHttpClient(); var status=client.send(HttpRequest.newBuilder(URI.create(CONFIG.uiUrl+"/auth/bootstrap/status")).build(),HttpResponse.BodyHandlers.ofString());
    if(status.body().contains("true")) { String body="{\"username\":\""+CONFIG.username+"\",\"email\":\"selenium-admin@example.test\",\"password\":\""+CONFIG.password+"\"}"; var response=client.send(HttpRequest.newBuilder(URI.create(CONFIG.uiUrl+"/auth/bootstrap")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString()); if(response.statusCode()!=200) throw new IllegalStateException("Disposable administrator setup failed: "+response.body()); }
  }
  @BeforeMethod(alwaysRun=true)
  public void openBrowser(Method method) {
    if (driver != null) throw new IllegalStateException("Previous browser was not cleaned up");
    ownerThread = Thread.currentThread().getId();
    driver = DriverFactory.create(CONFIG);
    session = ((RemoteWebDriver) driver).getSessionId().toString();
    if (!ACTIVE_SESSIONS.add(session)) throw new IllegalStateException("Browser session is shared: " + session);
    log("OPEN", method.getName());
    wait = new WebDriverWait(driver, Duration.ofSeconds(CONFIG.timeoutSeconds));
    new LoginPage(driver, wait, CONFIG.uiUrl).open().login(CONFIG.username, CONFIG.password);
  }

  @AfterMethod(alwaysRun=true)
  public void evidence(ITestResult result) throws Exception {
    if (driver == null) return;
    try {
      if (ownerThread != Thread.currentThread().getId()) {
        throw new IllegalStateException("Browser cleanup attempted from a different thread");
      }
      if (!result.isSuccess() && driver instanceof TakesScreenshot shots) {
        Path dir = CONFIG.root.resolve("target/artifacts").resolve(
            getClass().getSimpleName() + "-" + result.getMethod().getMethodName() + "-" + UUID.randomUUID());
        Files.createDirectories(dir);
        // A test may fail inside an iframe; switch back for the evidence capture.
        driver.switchTo().defaultContent();
        Files.write(dir.resolve("failure.png"), shots.getScreenshotAs(OutputType.BYTES));
        Files.writeString(dir.resolve("url.txt"), driver.getCurrentUrl());
        Files.writeString(dir.resolve("execution.txt"), "thread=" + ownerThread + " session=" + session);
        System.out.println("EVIDENCE " + dir);
      }
    } catch (Exception error) {
      System.err.println("Evidence/ownership failure: " + error);
      throw error;
    } finally {
      try {
        driver.quit();
        ACTIVE_SESSIONS.remove(session);
        log("CLOSED", result.getMethod().getMethodName());
      } finally {
        driver = null;
        wait = null;
      }
    }
  }

  private void log(String event, String method) {
    SESSION_EVENTS.add(event + "\t" + getClass().getSimpleName() + "." + method
        + "\t" + Thread.currentThread().getId() + "\t" + session + "\t" + Instant.now());
    System.out.println(event + " " + getClass().getSimpleName() + "." + method
        + " thread=" + Thread.currentThread().getId() + " session=" + session + " at=" + Instant.now());
  }

  @AfterSuite(alwaysRun=true)
  public void stopApplication() throws Exception {
    try {
      Files.createDirectories(CONFIG.root.resolve("target"));
      Files.write(CONFIG.root.resolve("target/browser-sessions.tsv"), SESSION_EVENTS);
      if (!ACTIVE_SESSIONS.isEmpty()) throw new IllegalStateException("Unclosed sessions: " + ACTIVE_SESSIONS);
    } finally {
      APP.close();
    }
  }
}
