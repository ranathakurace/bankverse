package com.bank.qa.tests.ui;

import com.bank.qa.core.BaseTest;
import com.microsoft.playwright.options.FilePayload;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Local HTML exercises teach tool APIs; they do not claim banking feature
 * coverage.
 */
public final class ToolPracticeTest extends BaseTest {
	@Test(groups = { "ui", "edge", "lab" })
	public void uploadSyntheticDocumentInLocalLab() {
		var page = test().page();
		page.setContent(
				"<label>Document<input type='file' data-testid='upload'></label><p id='name'></p><script>document.querySelector('input').onchange=e=>document.querySelector('#name').textContent=e.target.files[0].name</script>");
		page.getByTestId("upload").setInputFiles(new FilePayload("synthetic-kyc.txt", "text/plain",
				"Training data only".getBytes(StandardCharsets.UTF_8)));
		assertThat(page.locator("#name")).hasText("synthetic-kyc.txt");
	}

	@Test(groups = { "ui", "edge", "lab" })
	public void nativeConfirmDialogInLocalLab() {
		var page = test().page();
		page.setContent(
				"<button onclick=\"document.querySelector('p').textContent=confirm('Approve training action?')?'Approved':'Cancelled'\">Confirm</button><p></p>");
		page.onceDialog(dialog -> {
			assertEquals(dialog.type(), "confirm");
			dialog.dismiss();
		});
		page.getByText("Confirm",
				new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).click();
		assertThat(page.locator("p")).hasText("Cancelled");
	}

	@Test(groups = { "ui", "edge", "lab" })
	public void downloadCsvAndInspectBytesInLocalLab() throws Exception {
		var page = test().page();
		page.setContent(
				"<a download='training.csv' href='data:text/csv,reference%2Camount%0APW-001%2C125.50'>Download training CSV</a>");
		var download = page.waitForDownload(() -> page.getByText("Download training CSV").click());
		assertEquals(download.suggestedFilename(), "training.csv");
		assertTrue(Files.readString(download.path()).contains("PW-001,125.50"));
	}
}
