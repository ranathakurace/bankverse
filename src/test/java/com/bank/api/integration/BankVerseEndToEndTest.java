package com.bank.api.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import com.bank.api.BankingApiApplication;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Exercises static UI delivery, authentication, persisted workflow state and event delivery over HTTP. */
@SpringBootTest(classes = BankingApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BankVerseEndToEndTest extends AbstractTestNGSpringContextTests {
    @LocalServerPort private int port;
    private String token;
    private String customerNumber;
    private String customerEmail;

    @BeforeClass public void configureHttpClient() {
        RestAssured.baseURI = "http://localhost"; RestAssured.port = port;
        given().contentType(ContentType.JSON).body(map("username", "test-admin", "email", "test-admin@example.test", "password", "TestAdminPass123"))
                .when().post("/auth/bootstrap").then().statusCode(200);
        token = given().contentType(ContentType.JSON).body(map("username", "test-admin", "password", "TestAdminPass123"))
                .when().post("/auth/login").then().statusCode(200).body("accessToken", notNullValue()).body("roles", hasItem("ADMIN")).extract().path("accessToken");
    }

    @Test public void rejectsIncompleteLoginPayloadsAsBadRequests() {
        given().contentType(ContentType.JSON).body(map("password", "TestAdminPass123"))
                .when().post("/auth/login").then().statusCode(400).body("errorCode", equalTo("INVALID_REQUEST"));
        given().contentType(ContentType.JSON).body(map("username", "test-admin"))
                .when().post("/auth/login").then().statusCode(400).body("errorCode", equalTo("INVALID_REQUEST"));
        given().contentType(ContentType.JSON).body(map("username", "test-admin", "password", "wrong-password"))
                .when().post("/auth/login").then().statusCode(401).body("errorCode", equalTo("AUTHENTICATION_FAILED"));
    }

    @Test public void rejectsUnsupportedLoginMethodWithMethodNotAllowed() {
        given().when().get("/auth/login").then().statusCode(405)
                .header("Allow", org.hamcrest.Matchers.containsString("POST"))
                .body("errorCode", equalTo("METHOD_NOT_ALLOWED"));
    }

    @Test public void servesFrontendWithStableAutomationSelectors() {
        given().header("X-Correlation-ID", "ui-contract-001").when().get("/").then().statusCode(200)
                .header("X-Correlation-ID", equalTo("ui-contract-001"))
                .body(org.hamcrest.Matchers.containsString("data-testid=\"login-submit\""))
                .body(org.hamcrest.Matchers.containsString("data-testid=\"api-result-body\""))
                .body(org.hamcrest.Matchers.containsString("id=\"confirmation-modal\""))
                .body(org.hamcrest.Matchers.containsString("data-testid=\"automation-entitlements\""))
                .body(org.hamcrest.Matchers.containsString("data-testid=\"automation-frame\""))
                .body(org.hamcrest.Matchers.containsString("data-testid=\"banking-error-modal\""))
                .body(org.hamcrest.Matchers.containsString("id=\"audit-filter-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"customer-activation-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"payment-initiation-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"card-issue-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"loan-application-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"risk-assessment-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"event-publish-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"customer-login-form\""))
                .body(org.hamcrest.Matchers.containsString("id=\"customer-transfer-form\""));
        given().when().get("/automation-frame.html").then().statusCode(200)
                .body(org.hamcrest.Matchers.containsString("data-testid=\"frame-verify\""));
        given().when().get("/automation-popup.html").then().statusCode(200)
                .body(org.hamcrest.Matchers.containsString("data-testid=\"popup-confirm\""));
    }

    @Test(dependsOnMethods = "servesFrontendWithStableAutomationSelectors") public void authenticatesAndRejectsInvalidToken() {
        Response login = given().header("Authorization", "Bearer expired-browser-token").contentType(ContentType.JSON).body(map("username", "test-admin", "password", "TestAdminPass123"))
                .when().post("/auth/login").then().statusCode(200).body("accessToken", notNullValue()).body("refreshToken", notNullValue()).extract().response();
        String refreshToken = login.path("refreshToken");
        Response refreshed = given().contentType(ContentType.JSON).body(map("refreshToken", refreshToken))
                .when().post("/auth/refresh").then().statusCode(200).body("accessToken", notNullValue()).body("username", equalTo("test-admin")).body("roles", hasItem("ADMIN")).extract().response();
        org.testng.Assert.assertNotEquals(refreshed.path("refreshToken"), refreshToken, "Refresh rotation must issue a new token");
        given().contentType(ContentType.JSON).body(map("refreshToken", refreshToken))
                .when().post("/auth/refresh").then().statusCode(401).body("errorCode", equalTo("AUTHENTICATION_FAILED"));
        given().header("Authorization", "Bearer invalid-token").when().get("/api/v1/accounts/ACC000001").then()
                .statusCode(401).body("errorCode", equalTo("INVALID_ACCESS_TOKEN"));
    }

    @Test(dependsOnMethods = "authenticatesAndRejectsInvalidToken") public void enforcesRoleBoundariesForProtectedModules() {
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(map("username", "customer-officer", "email", "customer-officer@example.test", "password", "CustomerPass123", "roles", java.util.Collections.singleton("CUSTOMER_OFFICER")))
                .when().post("/api/v1/admin/employees").then().statusCode(201);
        String officerToken = given().contentType(ContentType.JSON).body(map("username", "customer-officer", "password", "CustomerPass123"))
                .when().post("/auth/login").then().statusCode(200).body("roles", hasItem("CUSTOMER_OFFICER")).extract().path("accessToken");
        given().header("Authorization", "Bearer " + officerToken).when().get("/api/v1/accounts/ACC000001")
                .then().statusCode(403).body("errorCode", equalTo("ACCESS_DENIED"));
    }

    @Test(dependsOnMethods = "enforcesRoleBoundariesForProtectedModules") public void completesCustomerKycActivationAndAccountWorkflow() {
        String unique = String.format("%09d", Math.abs(UUID.randomUUID().hashCode()) % 1_000_000_000);
        customerEmail = "qa" + unique + "@example.test";
        Response customer = given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON).body(map("fullName", "Automation Tester", "email", customerEmail, "phone", "9" + unique, "pan", "AUTOX" + unique.substring(0, 4) + "F", "aadhaar", "123" + unique, "dob", "1990-01-01"))
                .when().post("/api/v1/customers/register").then().statusCode(201).body("customerNumber", notNullValue()).extract().response();
        customerNumber = customer.path("customerNumber");
        String authorization = "Bearer " + token;
        given().header("Authorization", authorization).contentType(ContentType.JSON).body(map("documentReference", "DOC-" + unique, "submittedBy", "E2E-QA", "remarks", "Automated submission"))
                .when().post("/api/v1/customers/{customer}/kyc/submissions", customerNumber).then().statusCode(201).body("kycStatus", equalTo("SUBMITTED"));
        given().header("Authorization", authorization).contentType(ContentType.JSON).body(map("reviewedBy", "E2E-QA", "remarks", "Automated review"))
                .when().post("/api/v1/customers/{customer}/kyc/review/start", customerNumber).then().statusCode(200).body("kycStatus", equalTo("UNDER_REVIEW"));
        given().header("Authorization", authorization).contentType(ContentType.JSON).body(map("decision", "APPROVE", "reviewedBy", "E2E-QA", "remarks", "Automated approval"))
                .when().post("/api/v1/customers/{customer}/kyc/review", customerNumber).then().statusCode(200).body("kycStatus", equalTo("COMPLETED"));
        given().header("Authorization", authorization).contentType(ContentType.JSON).body(map("comments", "Automated activation"))
                .when().post("/api/v1/customers/{customer}/activate", customerNumber).then().statusCode(200).body("customerStatus", equalTo("ACTIVE"));
        given().header("Authorization", authorization).contentType(ContentType.JSON).body(map("customerNumber", customerNumber, "accountType", "SAVINGS", "currency", "INR"))
                .when().post("/api/v1/accounts").then().statusCode(201).body("accountNumber", notNullValue()).body("accountStatus", equalTo("ACTIVE"));
    }

    @Test(dependsOnMethods = "completesCustomerKycActivationAndAccountWorkflow") public void enrollsAndProtectsCustomerOnlineBanking() {
        String customerPassword = "CustomerPortal123";
        given().contentType(ContentType.JSON).body(map("customerNumber", customerNumber, "email", customerEmail, "password", customerPassword))
                .when().post("/customer-auth/enroll").then().statusCode(200).body("roles", hasItem("CUSTOMER"));
        String customerToken = given().contentType(ContentType.JSON).body(map("customerNumber", customerNumber, "password", customerPassword))
                .when().post("/customer-auth/login").then().statusCode(200).body("roles", hasItem("CUSTOMER")).extract().path("accessToken");
        Response accounts = given().header("Authorization", "Bearer " + customerToken).when().get("/api/v1/customer-portal/accounts").then().statusCode(200).extract().response();
        String accountNumber = accounts.path("[0].accountNumber");
        given().header("Authorization", "Bearer " + customerToken).when().get("/api/v1/customer-portal/accounts/{account}/transactions", accountNumber).then().statusCode(200);
        given().when().post("/customer-auth/password").then().statusCode(401);
        given().header("Authorization", "Bearer " + customerToken).when().get("/api/v1/admin/employees").then().statusCode(403);
    }

    @Test(dependsOnMethods = "enforcesRoleBoundariesForProtectedModules") public void pagesEmployeeDirectoryAndFiltersAndExportsSecurityAudit() {
        String authorization = "Bearer " + token;
        given().header("Authorization", authorization).queryParam("query", "customer-officer").queryParam("active", true).queryParam("page", 0).queryParam("size", 1)
                .when().get("/api/v1/admin/employees").then().statusCode(200).body("content.size()", equalTo(1)).body("content[0].username", equalTo("customer-officer")).body("totalElements", equalTo(1));
        given().header("Authorization", authorization).queryParam("employee", "test-admin").queryParam("action", "EMPLOYEE_CREATED").queryParam("page", 0).queryParam("size", 10)
                .when().get("/api/v1/admin/security-audit").then().statusCode(200).body("content.action", hasItem("EMPLOYEE_CREATED"));
        given().header("Authorization", authorization).queryParam("action", "EMPLOYEE_CREATED")
                .when().get("/api/v1/admin/security-audit/export").then().statusCode(200).contentType("text/csv").body(org.hamcrest.Matchers.containsString("occurredAt,action,actorUsername"));
    }

    @Test(dependsOnMethods = "completesCustomerKycActivationAndAccountWorkflow") public void persistsAndConsumesEventOutboxMessage() {
        String key = "e2e-event-" + UUID.randomUUID(); String authorization = "Bearer " + token;
        Response event = given().header("Authorization", authorization).header("Idempotency-Key", key).contentType(ContentType.JSON)
                .body(map("aggregateType", "CUSTOMER", "aggregateReference", customerNumber, "eventType", "CUSTOMER_ACTIVATED", "payload", "{\"customerNumber\":\"" + customerNumber + "\"}"))
                .when().post("/api/v1/events").then().statusCode(201).body("status", equalTo("PENDING")).extract().response();
        String eventId = event.path("eventId");
        given().header("Authorization", authorization).when().post("/api/v1/events/{eventId}/dispatch", eventId).then().statusCode(200).body("status", equalTo("PUBLISHED"));
        given().header("Authorization", authorization).contentType(ContentType.JSON).body(map("consumerName", "E2E-QA-CONSUMER"))
                .when().post("/api/v1/events/{eventId}/consume", eventId).then().statusCode(200).body("status", equalTo("PROCESSED"));
    }

    private Map<String, Object> map(Object... values) { Map<String, Object> map = new HashMap<>(); for (int i = 0; i < values.length; i += 2) map.put((String) values[i], values[i + 1]); return map; }
}
