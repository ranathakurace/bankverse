package com.bank.qa.tools;

import com.bank.qa.config.RunConfig;
import com.bank.qa.core.PlaywrightFactory;
import com.microsoft.playwright.*;
import java.nio.file.*;
import static org.testng.Assert.*;

/**
 * Optional maintainer utility; not a test and never starts the banking
 * application.
 */
public final class HandbookPreview {
	public static void main(String[] args) throws Exception {
		var config = new RunConfig();
		Path output = config.root.resolve("target/docs-preview");
		Files.createDirectories(output);
		try (var playwright = PlaywrightFactory.create(config);
				var browser = playwright.chromium()
						.launch(new BrowserType.LaunchOptions().setChannel("chromium"))) {
			var page = browser.newPage(new Browser.NewPageOptions().setViewportSize(1440, 1000));
			page.navigate(config.root.resolve("docs/handbook.html").toUri().toString());
			assertEquals(page.locator("figure svg").count(), 2);
			assertEquals(page.evaluate("document.documentElement.scrollWidth <= innerWidth"), true,
					"Desktop overflow");
			assertEquals(page.evaluate(
					"Array.from(document.querySelectorAll('a[href^=\"#\"]')).every(a=>document.querySelector(a.getAttribute('href')))"),
					true, "Broken chapter link");
			page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("desktop.png")));
			page.locator("#architecture figure").first().screenshot(
					new Locator.ScreenshotOptions().setPath(output.resolve("architecture.png")));
			page.locator("#architecture figure").last().screenshot(
					new Locator.ScreenshotOptions().setPath(output.resolve("execution.png")));
			page.setViewportSize(390, 900);
			page.evaluate("scrollTo(0,0)");
			assertEquals(page.evaluate("document.documentElement.scrollWidth <= innerWidth"), true,
					"Mobile overflow");
			page.screenshot(new Page.ScreenshotOptions().setPath(output.resolve("mobile.png")));
			System.out.println(
					"Offline handbook QA passed: desktop/mobile width, chapter links, and two embedded diagrams.");
		}
	}
}
