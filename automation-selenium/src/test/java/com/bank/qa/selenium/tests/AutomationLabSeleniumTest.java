package com.bank.qa.selenium.tests;

import com.bank.qa.selenium.core.BaseUiTest;
import com.bank.qa.selenium.pages.AutomationLabPage;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class AutomationLabSeleniumTest extends BaseUiTest {
    @Test(groups = {"smoke", "regression"})
    public void nativeControlsAndAlert() {
        var lab = new AutomationLabPage(driver, wait);
        lab.setRisk("HIGH");
        assertEquals(lab.risk(), "HIGH");
        lab.setRisk("LOW");
        assertEquals(lab.risk(), "LOW");
    }

    @Test(groups = "regression")
    public void entitlementSelectionCanBeCleared() {
        var lab = new AutomationLabPage(driver, wait);
        lab.selectEntitlements("CUSTOMER_READ", "AUDIT_VIEW");
        assertEquals(lab.selectedEntitlements(), 2);
        lab.selectEntitlements();
        assertEquals(lab.selectedEntitlements(), 0);
    }

    @Test(groups = "regression")
    public void checkerSelectionShowsApprovalWarning() {
        var lab = new AutomationLabPage(driver, wait);
        lab.chooseChecker();
        assertTrue(lab.isCheckerSelected());
        lab.showWarning();
        assertTrue(lab.alertText().contains("checker approval"));
    }
}
