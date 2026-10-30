package com.bank.qa.selenium.tests;

import com.bank.qa.selenium.core.BaseUiTest;
import com.bank.qa.selenium.pages.AutomationLabPage;
import java.time.Instant;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public final class AutomationLabSeleniumTest extends BaseUiTest {
    @Test(groups = {"smoke", "regression"})
    public void nativeControlsAndAlert() {
        String execution = "nativeControlsAndAlert"
                + " thread=" + Thread.currentThread().getId()
                + " session=" + ((RemoteWebDriver) driver).getSessionId();

        System.out.println("START " + execution + " at=" + Instant.now());

        try {
            var lab = new AutomationLabPage(driver, wait);

            lab.setRisk("HIGH");
            assertEquals(lab.risk(), "HIGH");

            lab.selectEntitlements("CUSTOMER_READ", "AUDIT_VIEW");
            assertEquals(lab.selectedEntitlements(), 2);

            lab.chooseChecker();
            assertTrue(lab.isCheckerSelected());

            lab.showWarning();
            assertTrue(lab.alertText().contains("checker approval"));
        } finally {
            System.out.println("END " + execution + " at=" + Instant.now());
        }
    }
}
