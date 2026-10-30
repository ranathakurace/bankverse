package com.bank.qa.data;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public record CustomerData(String fullName, String email, String phone, String pan, String aadhaar,
		String dob) {
	public static CustomerData fresh() {
		var random = ThreadLocalRandom.current();
		StringBuilder pan = new StringBuilder();
		for (int i = 0; i < 5; i++)
			pan.append((char) ('A' + random.nextInt(26)));
		pan.append(String.format("%04d", random.nextInt(10000)))
				.append((char) ('A' + random.nextInt(26)));
		return new CustomerData("Java Playwright Customer",
				"java-pw-" + UUID.randomUUID() + "@example.test",
				String.valueOf(random.nextLong(6000000000L, 10000000000L)), pan.toString(),
				String.valueOf(random.nextLong(100000000000L, 1000000000000L)),
				LocalDate.of(1990, 6, 15).toString());
	}

	public Map<String, Object> payload() {
		var data = new LinkedHashMap<String, Object>();
		data.put("fullName", fullName);
		data.put("email", email);
		data.put("phone", phone);
		data.put("pan", pan);
		data.put("aadhaar", aadhaar);
		data.put("dob", dob);
		return data;
	}
}
