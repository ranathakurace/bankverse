# BankVerse

BankVerse is a modular Spring Boot banking-learning platform. It provides an operations console, a customer portal, protected REST APIs, PostgreSQL persistence, and Java automation suites for practicing API, UI, security, database, and CI/CD testing.

> **Learning project, not a production banking system.** Do not use real customer data, production credentials, or real funds.

## Current capabilities

| Area | Implemented capabilities |
| --- | --- |
| Identity and access | One-time administrator bootstrap; employee directory; search and server-side pagination; multiple role assignment; active/inactive status; employee password reset; employee password change; BCrypt password storage; JWT access and refresh tokens; RBAC permissions; correlation IDs; security audit history and CSV export. |
| Customer and KYC | Customer registration, lookup and search; KYC submission, review start, approve/reject decision, history, and customer activation. |
| Accounts and ledger | Account opening, account/customer lookup, freeze, unfreeze, close, statement lookup, deposits, withdrawals, account-to-account transfers, and ledger history. |
| Payments | Idempotent payment initiation plus authorize, execute, fail, retry, refund, and lookup workflows. |
| Cards and loans | Card issuance, lookup, activate, block, unblock, close; loan application, lookup, approve, reject, and idempotent disbursement. |
| Enterprise controls | Risk-assessment creation, control-case lookup and resolution, and notification handling. |
| Event operations | Durable outbox event publication, lookup, dispatch, failure, retry, and idempotent consumer receipts. This is an event-driven foundation; an external Kafka/RabbitMQ broker is not bundled. |
| Customer portal | Customer online-access enrollment and login, password change, account/transaction views, and protected transfers. |
| Operations UI | Static Operations Console pages for the implemented workflows, including employee administration, API-workspace templates, confirmations, alerts, dialogs, iframe and popup test surfaces, and customer-facing pages. |

## Architecture

```text
Operations Console / Customer Portal / Postman / Test Automation
                         |
                         v
              Spring MVC controllers + validation
                         |
                         v
    Domain services (KYC, accounts, payments, cards, loans, controls)
        |                 |                    |
        v                 v                    v
 JWT/RBAC + audit   Event outbox/receipts   JPA repositories
                                                  |
                                                  v
                                             PostgreSQL
```

Cross-cutting controls include JWT authentication, role/permission checks, BCrypt passwords, security audit records, correlation IDs, idempotency keys on applicable write operations, and a consistent error response model.

## API modules

The UI includes an API workspace with reusable templates. These route groups are also suitable for Postman and Rest Assured practice.

| Module | Base route |
| --- | --- |
| Health | `GET /health` |
| Employee authentication | `/auth/bootstrap`, `/auth/login`, `/auth/refresh`, `/auth/password` |
| Employee administration | `/api/v1/admin/employees`, `/api/v1/admin/security-audit` |
| Customers and KYC | `/api/v1/customers` |
| Accounts | `/api/v1/accounts` |
| Ledger transactions | `/api/v1/transactions` |
| Payments | `/api/v1/payments` |
| Cards | `/api/v1/cards` |
| Loans | `/api/v1/loans` |
| Enterprise controls | `/api/v1/controls` |
| Event operations | `/api/v1/events` |
| Customer authentication | `/customer-auth` |
| Customer portal | `/api/v1/customer-portal` |

Endpoint-level request formats and sample payloads are available in the Operations Console API workspace and the controller/request classes under `src/main/java`.

## Technology

- Java 11 application baseline; CI and automation use JDK 17.
- Spring Boot 2.7.18, Spring Security, Spring Data JPA, Hibernate, Maven.
- PostgreSQL for local runtime; isolated H2 configuration for tests.
- JavaScript/CSS static web UI served by Spring Boot.
- TestNG, Playwright Java, Selenium, Rest Assured, JaCoCo, OWASP Dependency-Check, GitHub Actions, and Jenkins/Docker assets.

## Local launch

### 1. Prepare PostgreSQL

Start PostgreSQL and create a local `bankverse` database. Configure the application process with these required environment variables. Do not put secrets in source control.

```powershell
$env:BANKVERSE_DB_URL = "jdbc:postgresql://localhost:5432/bankverse"
$env:BANKVERSE_DB_USERNAME = "postgres"
$env:BANKVERSE_DB_PASSWORD = "replace-with-your-postgres-password"
$env:BANKVERSE_JWT_SECRET = "replace-with-a-random-secret-at-least-32-characters-long"
```

Full guidance, including an Eclipse run configuration, is in [Runtime configuration](docs/RUNTIME_CONFIGURATION.md).

### 2. Start the application

With Maven installed and on your `PATH`:

```powershell
mvn spring-boot:run
```

Or in Eclipse, run `com.bank.api.BankingApiApplication` after adding the same four environment variables to **Run Configurations → Environment**.

### 3. Open BankVerse

- Operations Console: <http://localhost:8080/>
- Health check: <http://localhost:8080/health>

On a new database, use the one-time bootstrap flow to create the first administrator. After that, sign in with the administrator account and manage employees and their roles through **Identity & Access**. The application deliberately does not seed a permanent default password.

## Test suites

### Application unit and integration tests

```powershell
mvn test
```

The root suite uses isolated H2 test configuration; it does not require local PostgreSQL credentials.

### Playwright Java acceptance suite

From [`automation-java`](automation-java):

```powershell
.\scripts\run.ps1 -Suite api
.\scripts\run.ps1 -Suite all -InstallBrowsers
.\scripts\run.ps1 -Suite ui -Browser firefox -InstallBrowsers
```

The managed suite starts BankVerse with a disposable H2 database on `http://127.0.0.1:18080`. See the [Playwright framework guide](automation-java/README.md).

### Selenium UI suite

From [`automation-selenium`](automation-selenium):

```powershell
mvn clean test -Psmoke
mvn test -Pregression
```

The suite exercises enterprise UI controls such as native selects, multi-selects, radio buttons, checkboxes, alerts, iframe switching, popup windows, and error dialogs. See the [Selenium framework guide](automation-selenium/README.md).

### Manual API testing

1. Call `GET /health` and expect `200`.
2. Bootstrap an administrator once, then call `POST /auth/login`.
3. Use the returned bearer token for protected routes.
4. Exercise an end-to-end customer flow: register → submit KYC → start review → approve → activate → open account → deposit/transfer.
5. Validate expected negative paths: invalid body `400`, invalid credentials `401`, insufficient role `403`, missing resource `404`, and unsupported method `405`.
6. Test payment/card/loan/control/event state transitions through the API workspace or Postman, sending an `Idempotency-Key` wherever the endpoint requires one.

## Quality gates and CI/CD

GitHub Actions runs the root build, unit/integration tests, JaCoCo coverage, static analysis, OWASP dependency scanning, and cross-browser Playwright acceptance tests. The Dependency-Check workflow uses an NVD API key when available, caches vulnerability data, retries transient NVD failures, and has a guarded data-feed fallback for NVD availability issues.

- [CI quality gates](docs/CI_QUALITY_GATES.md)
- [Jenkins on Ubuntu with Docker](docs/JENKINS_UBUNTU_DOCKER.md)
- [`Jenkinsfile`](Jenkinsfile)
- [GitHub Actions workflows](.github/workflows)

For GitHub Actions, configure `NVD_API_KEY` as a repository secret. The key is never committed to the repository.

## Repository layout

```text
src/                    Spring Boot application and root test suite
automation-java/        Playwright Java API and cross-browser UI acceptance tests
automation-selenium/    Selenium Java UI framework
automation/             Earlier TypeScript proof of concept
docs/                   Runtime, quality-gate, and Jenkins documentation
ci/jenkins/             Docker assets for a Jenkins controller/agent setup
.github/workflows/      GitHub Actions quality and acceptance pipelines
```

## Security and data-handling notes

- Runtime database credentials and JWT signing secrets are supplied through environment variables.
- `.env` files, access tokens, passwords, and secrets must not be committed.
- Application test configuration is separate from local runtime configuration.
- Use disposable databases and dedicated QA accounts for automated write tests.
- Never point the learning automation suites at a production system.

## Project status

The repository currently provides a comprehensive **modular-monolith and event-outbox foundation** for learning. A future production-grade decomposition would require separate deployable services, a real message broker, centralized observability, database migrations/governance, production secrets management, resilience testing, and operational runbooks.
