package com.bank.qa.tests.ui;

import com.bank.qa.core.BaseTest;
import com.microsoft.playwright.options.SelectOption;
import org.testng.annotations.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class AutomationLabTest extends BaseTest {
	@Test(groups = { "ui", "smoke" })
	public void selectsRadiosAndCheckboxes() {
		var page = test().signedInPage();
		page.getByTestId("automation-risk-tier").selectOption("HIGH");
		assertThat(page.getByTestId("automation-risk-tier")).hasValue("HIGH");
		page.getByTestId("automation-entitlements")
				.selectOption(new String[] { "CUSTOMER_READ", "AUDIT_VIEW" });
		assertThat(page.getByTestId("automation-entitlements"))
				.hasValues(new String[] { "CUSTOMER_READ", "AUDIT_VIEW" });
		page.getByTestId("automation-route-checker").check();
		assertThat(page.getByTestId("automation-route-checker")).isChecked();
		page.getByTestId("automation-override").check();
		assertThat(page.getByTestId("automation-override")).isChecked();
		page.getByTestId("automation-show-warning").click();
		assertThat(page.getByTestId("automation-alert")).containsText("checker approval");
	}

	@Test(groups = { "ui", "edge" })
	public void verificationInsideFrame() {
		var frame = test().signedInPage().frameLocator("[data-testid='automation-frame']");
		frame.getByTestId("frame-verification-method")
				.selectOption(new SelectOption().setLabel("Document review"));
		frame.getByTestId("frame-reference").fill("JAVA-VERIFICATION");
		frame.getByTestId("frame-consent").check();
		frame.getByTestId("frame-verify").click();
		assertThat(frame.getByTestId("frame-alert")).containsText("verification completed");
	}

	@Test(groups = { "ui", "edge" })
	public void popupLifecycle() {
		var page = test().signedInPage();
		try (var popup = page
				.waitForPopup(() -> page.getByTestId("automation-open-popup").click())) {
			popup.getByTestId("popup-priority").selectOption(new SelectOption().setLabel("High"));
			popup.getByTestId("popup-reference").fill("JAVA-CASE");
			popup.getByTestId("popup-confirm").click();
			assertThat(popup.getByTestId("popup-status")).containsText("Popup action confirmed");
		}
		assertThat(page.getByTestId("nav-dashboard")).isVisible();
	}

	@Test(groups = { "ui", "edge" })
	public void customBankingErrorDialog() {
		var page = test().signedInPage();
		page.getByTestId("automation-show-error").click();
		assertThat(page.getByTestId("banking-error-modal"))
				.containsText("simulated duplicate transaction");
		page.getByTestId("banking-error-close").click();
		assertThat(page.getByTestId("banking-error-modal")).isHidden();
	}
}
