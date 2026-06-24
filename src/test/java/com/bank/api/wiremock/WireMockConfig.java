package com.bank.api.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockConfig {

    private static WireMockServer wireMockServer;

    @BeforeSuite
    public void startWireMock() {
        wireMockServer = new WireMockServer(9999);
        wireMockServer.start();

        configureFor("localhost", 9999);

        // Non-fraud account
        stubFor(get(urlPathEqualTo("/fraud/check"))
                .withQueryParam("accountId", equalTo("1"))
                .willReturn(okJson(
                        "{ \"fraudulent\": false, \"reason\": \"\" }"
                )));

        // Fraud account
        stubFor(get(urlPathEqualTo("/fraud/check"))
                .withQueryParam("accountId", equalTo("3"))
                .willReturn(okJson(
                        "{ \"fraudulent\": true, \"reason\": \"Suspicious activity\" }"
                )));
    }

    @AfterSuite
    public void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}
