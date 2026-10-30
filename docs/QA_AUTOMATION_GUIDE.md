# BankVerse QA Automation Guide

## Test entry points

- UI: `http://localhost:8080/`
- Health: `GET /health`
- Authentication: `POST /auth/login`
- Correlation header: send `X-Correlation-ID: qa-run-001`; the server always returns it.

Use the learning account `admin/password` only for the current learning build. Use a unique customer PAN, email, phone, Aadhaar number, and `Idempotency-Key` for every repeatable test.

## Selenium / Playwright stable selectors

| Action | Selector |
|---|---|
| Login form | `[data-testid="login-form"]` |
| Username | `[data-testid="login-username"]` |
| Password | `[data-testid="login-password"]` |
| Submit login | `[data-testid="login-submit"]` |
| Dashboard menu | `[data-testid="nav-dashboard"]` |
| Customer menu | `[data-testid="nav-customers"]` |
| Account menu | `[data-testid="nav-accounts"]` |
| Customer registration | `[data-testid="customer-registration-form"]` |
| Customer submit | `[data-testid="customer-submit"]` |
| Account opening | `[data-testid="account-opening-form"]` |
| API execute | `[data-testid="api-execute"]` |
| API result | `[data-testid="api-result-body"]` |
| Logout | `[data-testid="logout"]` |

Do not use visual position or CSS class selectors for functional automation. Wait for the API result panel or the active navigation item instead of fixed sleeps.

## API assertions

1. `POST /auth/login` with valid credentials returns `200` and an `accessToken`.
2. An invalid bearer token on any protected endpoint returns `401` and error code `INVALID_ACCESS_TOKEN`.
3. A request without a token to a protected endpoint returns `401` and error code `AUTHENTICATION_REQUIRED`.
4. Any request returns `X-Correlation-ID`; it matches the caller value when supplied.
5. Repeat a deposit, transfer, payment, loan-disbursement, or outbox publish call with the same idempotency key; verify the same business result and no duplicate side effect.
6. Exercise workflow conflicts: activate before KYC completion, consume a pending event, or close an account with a balance. Expect a documented `409` business-state response.

## Current architecture boundary

BankVerse is one Spring Boot application with PostgreSQL persistence and a durable outbox foundation. It is not yet independently deployable microservices, so contract tests between separately hosted services and broker-container tests are future work. The outbox endpoints provide a safe starting point for API, retry, idempotency, and consumer-receipt tests today.
