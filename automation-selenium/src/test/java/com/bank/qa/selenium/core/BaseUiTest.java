package com.bank.qa.selenium.core;

import com.bank.qa.selenium.config.RunConfig;
import com.bank.qa.selenium.pages.LoginPage;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.*;

public abstract class BaseUiTest {
  protected static final RunConfig CONFIG=new RunConfig();
  private static final ManagedApplication APP=new ManagedApplication();
  protected WebDriver driver; protected WebDriverWait wait;
  @BeforeSuite(alwaysRun=true) public void startApplication() throws Exception { APP.start(CONFIG); bootstrapAdminIfRequired(); }
  private void bootstrapAdminIfRequired() throws Exception {
    if(!CONFIG.managed) return;
    var client=HttpClient.newHttpClient(); var status=client.send(HttpRequest.newBuilder(URI.create(CONFIG.uiUrl+"/auth/bootstrap/status")).build(),HttpResponse.BodyHandlers.ofString());
    if(status.body().contains("true")) { String body="{\"username\":\""+CONFIG.username+"\",\"email\":\"selenium-admin@example.test\",\"password\":\""+CONFIG.password+"\"}"; var response=client.send(HttpRequest.newBuilder(URI.create(CONFIG.uiUrl+"/auth/bootstrap")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString()); if(response.statusCode()!=200) throw new IllegalStateException("Disposable administrator setup failed: "+response.body()); }
  }
  @BeforeMethod(alwaysRun=true) public void openBrowser() { driver=DriverFactory.create(CONFIG); wait=new WebDriverWait(driver,Duration.ofSeconds(CONFIG.timeoutSeconds)); new LoginPage(driver,wait,CONFIG.uiUrl).open().login(CONFIG.username,CONFIG.password); }
  @AfterMethod(alwaysRun=true) public void evidence(ITestResult result) { if(driver==null)return; try { if(!result.isSuccess() && driver instanceof TakesScreenshot shots) { Path dir=CONFIG.root.resolve("target/artifacts").resolve(result.getMethod().getMethodName()+"-"+UUID.randomUUID()); Files.createDirectories(dir); Files.write(dir.resolve("failure.png"),shots.getScreenshotAs(OutputType.BYTES)); Files.writeString(dir.resolve("url.txt"),driver.getCurrentUrl()); } } catch(Exception ignored) {} finally { driver.quit(); driver=null; } }
  @AfterSuite(alwaysRun=true) public void stopApplication() { APP.close(); }
}
