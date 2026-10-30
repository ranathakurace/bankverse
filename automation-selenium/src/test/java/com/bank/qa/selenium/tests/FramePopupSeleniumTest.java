package com.bank.qa.selenium.tests;

import com.bank.qa.selenium.core.BaseUiTest;
import com.bank.qa.selenium.pages.AutomationLabPage;
import java.time.Instant;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.Test;

public final class FramePopupSeleniumTest extends BaseUiTest {

    @Test(groups = "regression")
    public void framePopupAndErrorDialog() {
        String execution = "framePopupAndErrorDialog"
                + " thread=" + Thread.currentThread().getId()
                + " session=" + ((RemoteWebDriver) driver).getSessionId();

        System.out.println("START " + execution + " at=" + Instant.now());

        try {
            var lab = new AutomationLabPage(driver, wait);

            lab.verifyInsideFrame();
            lab.checkPopup();
            lab.showAndCloseError();
        } finally {
            System.out.println("END " + execution + " at=" + Instant.now());
        }
    }
}
