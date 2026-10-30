package com.bank.qa.core;

import com.google.gson.*;
import com.microsoft.playwright.APIResponse;

public final class Json {
	public static final Gson GSON = new GsonBuilder().serializeNulls().create();

	private Json() {
	}

	public static JsonObject body(APIResponse response) {
		return JsonParser.parseString(response.text()).getAsJsonObject();
	}
}
