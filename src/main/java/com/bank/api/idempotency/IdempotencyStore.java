package com.bank.api.idempotency;

import com.bank.api.dto.TransferResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyStore {

    // key → response
    private static final Map<String, TransferResponse> store =
            new ConcurrentHashMap<>();

    public static boolean exists(String key) {
        return store.containsKey(key);
    }

    public static TransferResponse get(String key) {
        return store.get(key);
    }

    public static void save(String key, TransferResponse response) {
        store.put(key, response);
    }
}
