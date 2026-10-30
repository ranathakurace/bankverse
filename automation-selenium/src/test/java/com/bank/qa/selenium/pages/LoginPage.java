package com.bank.qa.selenium.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public final class LoginPage {
  private final WebDriver driver; private final WebDriverWait wait; private final String baseUrl;
  public LoginPage(WebDriver driver,WebDriverWait wait,String baseUrl){this.driver=driver;this.wait=wait;this.baseUrl=baseUrl;}
  public LoginPage open(){driver.get(baseUrl+"/");wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='login-form']")));return this;}
  public void login(String username,String password){driver.findElement(By.cssSelector("[data-testid='login-username']")).sendKeys(username);driver.findElement(By.cssSelector("[data-testid='login-password']")).sendKeys(password);driver.findElement(By.cssSelector("[data-testid='login-submit']")).click();/* The responsive layout hides the sidebar footer; dashboard navigation remains visible after login. */wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='nav-dashboard']")));}
}
