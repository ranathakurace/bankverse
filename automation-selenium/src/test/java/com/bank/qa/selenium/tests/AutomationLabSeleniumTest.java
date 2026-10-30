package com.bank.qa.selenium.tests;

import com.bank.qa.selenium.core.BaseUiTest;
import com.bank.qa.selenium.pages.AutomationLabPage;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class AutomationLabSeleniumTest extends BaseUiTest {
  @Test(groups={"smoke","regression"}) public void nativeControlsAndAlert(){var lab=new AutomationLabPage(driver,wait);lab.setRisk("HIGH");assertEquals(lab.risk(),"HIGH");lab.selectEntitlements("CUSTOMER_READ","AUDIT_VIEW");assertEquals(lab.selectedEntitlements(),2);lab.chooseChecker();assertTrue(lab.isCheckerSelected());lab.showWarning();assertTrue(lab.alertText().contains("checker approval"));}
  @Test(groups="regression") public void framePopupAndErrorDialog(){var lab=new AutomationLabPage(driver,wait);lab.verifyInsideFrame();lab.checkPopup();lab.showAndCloseError();}
}
