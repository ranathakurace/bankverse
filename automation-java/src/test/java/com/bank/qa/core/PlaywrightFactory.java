package com.bank.qa.core;

import com.bank.qa.config.RunConfig;
import com.microsoft.playwright.Playwright;
import java.util.HashMap;

public final class PlaywrightFactory {
	private PlaywrightFactory() {
	}

	public static Playwright create(RunConfig config) {
		var env = new HashMap<>(System.getenv());
		env.putIfAbsent("PLAYWRIGHT_BROWSERS_PATH", config.root.resolve(".browsers").toString());
		env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
		return Playwright.create(new Playwright.CreateOptions().setEnv(env));
	}
}
