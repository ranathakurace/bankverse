package com.bank.qa.tests.exercises;

import com.bank.qa.core.BaseTest;
import org.testng.annotations.Test;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Intentionally fails only when explicitly selected; never included in
 * default/CI runs.
 */
public final class FailureEvidenceExerciseTest extends BaseTest {
	@Test(groups = {
			"exercise" }, description = "Intentional failure for learning screenshots and Trace Viewer")
	public void intentionalFailureForTracePractice() {
		var page = test().signedInPage();
		assertThat(page.getByTestId("session-user")).hasText("INTENTIONALLY_WRONG_USERNAME");
	}
}
