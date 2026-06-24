package com.bank.api.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RefreshTokenStore {

    // username → refreshToken
    private static final Map<String, String> store = new ConcurrentHashMap<>();

    public static void save(String username, String refreshToken) {
        store.put(username, refreshToken);
    }

    public static String get(String username) {
        return store.get(username);
    }

    public static void remove(String username) {
        store.remove(username);
    }
}
