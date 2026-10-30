package com.bank.qa.selenium.tests;

import com.bank.qa.selenium.core.BaseUiTest;
import com.bank.qa.selenium.pages.AutomationLabPage;
import org.testng.annotations.Test;

public final class FramePopupSeleniumTest extends BaseUiTest {
    @Test(groups = "regression")
    public void iframeVerificationCompletes() {
        new AutomationLabPage(driver, wait).verifyInsideFrame();
    }

    @Test(groups = "regression")
    public void popupActionCompletesAndReturnsToParent() {
        new AutomationLabPage(driver, wait).checkPopup();
    }

    @Test(groups = "regression")
    public void errorDialogCanBeAcknowledged() {
        new AutomationLabPage(driver, wait).showAndCloseError();
    }
}
