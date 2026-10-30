package com.bank.qa.tests.framework;

import com.bank.qa.config.RunConfig;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Explicit opt-in infrastructure check. It never starts or contacts an
 * application.
 */
public final class ExternalWriteGuardTest {
	@Test(groups = { "exercise", "guard" })
	public void readOnlyProbeRemainsSelected() {
		var config = new RunConfig();
		assertFalse(config.managed, "Run this probe with -DstartApp=false");
		assertFalse(config.allowTestData, "Run with external writes disabled");
	}

	@Test(groups = { "exercise", "guard", "write" })
	public void guardedMutationMustNotRun() {
		fail("WriteGuard failed to exclude a write-tagged external test");
	}
}
