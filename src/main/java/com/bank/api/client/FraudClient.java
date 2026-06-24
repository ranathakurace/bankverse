package com.bank.api.client;

import com.bank.api.dto.FraudResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FraudClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public FraudResponse checkFraud(int accountId) {

        String url = "http://localhost:9999/fraud/check?accountId=" + accountId;

        return restTemplate.getForObject(url, FraudResponse.class);
    }
}
