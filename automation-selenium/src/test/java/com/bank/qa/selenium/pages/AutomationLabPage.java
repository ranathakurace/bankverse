package com.bank.qa.selenium.pages;

import java.util.Set;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

public final class AutomationLabPage {
  private final WebDriver driver; private final WebDriverWait wait;
  public AutomationLabPage(WebDriver driver,WebDriverWait wait){this.driver=driver;this.wait=wait;}
  private By id(String value){return By.cssSelector("[data-testid='"+value+"']");}
  public void setRisk(String risk){new Select(driver.findElement(id("automation-risk-tier"))).selectByValue(risk);}
  public String risk(){return new Select(driver.findElement(id("automation-risk-tier"))).getFirstSelectedOption().getDomAttribute("value");}
  public void selectEntitlements(String... values){var select=new Select(driver.findElement(id("automation-entitlements")));select.deselectAll();for(String value:values)select.selectByValue(value);}
  public int selectedEntitlements(){return new Select(driver.findElement(id("automation-entitlements"))).getAllSelectedOptions().size();}
  public void chooseChecker(){driver.findElement(id("automation-route-checker")).click();}
  public boolean isCheckerSelected(){return driver.findElement(id("automation-route-checker")).isSelected();}
  public void showWarning(){driver.findElement(id("automation-show-warning")).click();}
  public String alertText(){return driver.findElement(id("automation-alert")).getText();}
  public void verifyInsideFrame(){wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(id("automation-frame")));new Select(driver.findElement(id("frame-verification-method"))).selectByVisibleText("Document review");driver.findElement(id("frame-reference")).sendKeys("SEL-VER-001");driver.findElement(id("frame-consent")).click();driver.findElement(id("frame-verify")).click();wait.until(ExpectedConditions.textToBePresentInElementLocated(id("frame-alert"),"verification completed"));driver.switchTo().defaultContent();}
  public void checkPopup(){String original=driver.getWindowHandle();driver.findElement(id("automation-open-popup")).click();wait.until(d->d.getWindowHandles().size()==2);Set<String> handles=driver.getWindowHandles();handles.stream().filter(h->!h.equals(original)).findFirst().ifPresent(driver.switchTo()::window);new Select(driver.findElement(id("popup-priority"))).selectByVisibleText("High");driver.findElement(id("popup-reference")).sendKeys("SEL-CASE-001");driver.findElement(id("popup-confirm")).click();wait.until(ExpectedConditions.textToBePresentInElementLocated(id("popup-status"),"Popup action confirmed"));driver.close();driver.switchTo().window(original);}
  public void showAndCloseError(){driver.findElement(id("automation-show-error")).click();wait.until(ExpectedConditions.visibilityOfElementLocated(id("banking-error-modal")));driver.findElement(id("banking-error-close")).click();wait.until(ExpectedConditions.invisibilityOfElementLocated(id("banking-error-modal")));}
}
