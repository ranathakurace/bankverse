package com.bank.qa.selenium.core;

import com.bank.qa.selenium.config.RunConfig;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.edge.*;
import org.openqa.selenium.firefox.*;

public final class DriverFactory {
  private DriverFactory() {}
  public static WebDriver create(RunConfig config) {
    WebDriver driver;
    switch (config.browser) {
      case "firefox" -> { FirefoxOptions options=new FirefoxOptions(); if(config.headless) options.addArguments("-headless"); driver=new FirefoxDriver(options); }
      case "edge" -> { EdgeOptions options=new EdgeOptions(); if(config.headless) options.addArguments("--headless=new"); options.addArguments("--window-size=1440,1000","--disable-dev-shm-usage"); driver=new EdgeDriver(options); }
      default -> { ChromeOptions options=new ChromeOptions(); if(config.headless) options.addArguments("--headless=new"); options.addArguments("--window-size=1440,1000","--no-sandbox","--disable-dev-shm-usage"); driver=new ChromeDriver(options); }
    }
    driver.manage().timeouts().implicitlyWait(Duration.ZERO); driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
    // Keep the declared desktop viewport in headless CI. Calling maximize() there
    // can reduce Chrome to its default 800px window and trigger mobile breakpoints.
    if (!config.headless) driver.manage().window().maximize();
    return driver;
  }
}
