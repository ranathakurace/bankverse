package com.bank.qa.core;

import com.bank.qa.config.RunConfig;
import java.util.*;
import org.testng.*;

public final class WriteGuard implements IMethodInterceptor {
	@Override
	public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
		if (new RunConfig().allowTestData)
			return methods;
		var selected = methods.stream()
				.filter(m -> !Arrays.asList(m.getMethod().getGroups()).contains("write")).toList();
		System.out.println("External environment: excluded " + (methods.size() - selected.size())
				+ " write methods.");
		if (selected.isEmpty())
			throw new IllegalStateException(
					"No eligible tests remain; select read-only groups or opt into ALLOW_TEST_DATA");
		return selected;
	}
}
