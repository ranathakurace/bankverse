> Historical TypeScript proof of concept. The primary Java framework and CI entry point are now [automation-java](../automation-java/README.md).

# BankVerse Playwright automation

One TypeScript suite tests the real Spring Boot UI and REST APIs. API tests run without a browser. UI tests run in Chromium, Firefox, and WebKit, and include UI/API persistence checks.

## Quick start

Requires Node.js 22+, Java 17+, Maven 3.9+, and access to npm, Maven, and Playwright download servers.

From the repository root:

```powershell
cd automation
npm ci
npx playwright install chromium firefox webkit
npm run typecheck
npm test
npm run report
```

The default run starts a separate Spring Boot process on **127.0.0.1:18080**, uses the existing test-scoped H2 dependency, creates an administrator, runs tests, and stops the process. Its database is discarded when the JVM exits. It never uses the developer PostgreSQL database. A server already occupying this port causes failure instead of accidental reuse. Maven's first run can take longer while dependencies download; readiness has a 180-second timeout.

The managed test password is a disposable fixture. It is not an application credential. No .env file is required for the default run.

## Commands

| Command | Purpose |
| --- | --- |
| `npm test` | API plus all three browser projects |
| `npm run test:api` | REST tests without browser installation |
| `npm run test:ui` | Chromium UI tests |
| `npm run test:cross-browser` | UI tests on all three browsers |
| `npm run test:smoke` | Tests tagged @smoke, including setup |
| `npm run test:headed` | Visible Chromium |
| `npm run test:debug` | Playwright Inspector |
| `npm run test:list` | Discover cases without starting the application |
| `npm run typecheck` | Strict TypeScript validation |
| `npm run report` | Open the HTML report |

Pass native Playwright arguments with `npm test -- --project=api --grep customer`. CI uses one retry and fails if a case only passes on retry. For local browser downloads in a restricted Windows environment, set `$env:PLAYWRIGHT_BROWSERS_PATH = "$PWD/.browsers"` before both installation and test execution.

## Existing QA environment

Copy `.env.example` to `.env`, then configure:

```dotenv
START_APP=false
API_BASE_URL=https://qa.bankverse.example
UI_BASE_URL=https://qa.bankverse.example
TEST_USERNAME=dedicated-test-admin
TEST_PASSWORD=inject-from-your-secret-store
ALLOW_TEST_DATA=false
```

Process environment variables override .env. URLs must be HTTP(S) origins; API paths are rooted at /. TLS certificate verification stays enabled. The UI host must serve or proxy its own API routes because the current frontend calls relative URLs.

An existing environment must already have a dedicated ADMIN employee. Setup checks health and authenticates but never bootstraps an external environment. Set `ALLOW_TEST_DATA=true` to include @write cases that create customer records. With false, registration, duplicate, malformed payload, and validation cases are excluded. Sign-in still issues tokens. Choose a QA database, never a production target.

There is no supported customer deletion endpoint. Unique synthetic PAN, Aadhaar, phone, and email values isolate cases across workers and retries; external runs retain their customer records and deactivated role-test employees. Use an ephemeral QA database or reset it through your environment's approved retention process. Random constrained identifiers have a small collision probability; run at bounded concurrency. Managed mode discards everything at JVM shutdown.

## Structure and extension

```text
config/environment.ts           Validated runtime settings
playwright.config.ts           Projects, retries, reports, web server
scripts/start-app.cjs          Disposable application launcher
src/api/                       Domain API clients and auth
src/assertions/                 Customer privacy and error contracts
src/data/                       Synthetic per-test data
src/fixtures/test.ts            Request contexts, sessions, page objects
src/pages/                      UI actions using stable test IDs
tests/environment.setup.ts      Health, bootstrap, credential preflight
tests/api/                      API acceptance tests
tests/ui/                       UI and UI/API acceptance tests
```

Import `test` and `expect` from `src/fixtures/test`. Use `customers` for authenticated customer API actions, `anonymousApi` for security tests, `customer` for fresh registration data, and `signedInPage` for an authenticated UI. Put new banking endpoints into domain clients and reusable UI actions into page objects. Keep business assertions in tests; extend contract assertions when response fields change. Tag persistent mutations @write and critical journeys @smoke.

Each test has a fresh browser context and API request context. Worker-scoped login avoids repeating authentication for every case. BankVerse uses sessionStorage, so the authenticated UI fixture injects only the current worker's session at the UI origin. A separate real login/logout test verifies the sign-in journey. Do not use that fixture for logout/reload assertions: its init script intentionally rehydrates authentication on navigation.

Use role/label/test-ID locators and Playwright's auto-waiting assertions. Start response and popup waits before the action that triggers them. Avoid fixed sleeps, serial test dependencies, shared customer identifiers, and generic retry wrappers around banking mutations. Add account, transfer, payment, KYC, and role-specific workflows using fresh prerequisites and separate least-privilege users as those suites grow.

H2 is fast acceptance infrastructure, not proof of PostgreSQL-specific behavior. Run external-mode coverage against a disposable PostgreSQL QA deployment before release. The initial suite covers authentication, authorization failures, customers, cross-layer persistence, and the automation lab; it includes a customer-officer access boundary test but does not yet cover every banking module or the full role permission matrix.

## Reporting and CI

HTML is written to `playwright-report/`, JUnit XML to `test-results/junit.xml`. UI failures retain traces and screenshots; API traces are disabled to reduce credential exposure. Auth bodies are never attached by custom code. Playwright UI traces can still contain tokens, form values, and network payloads, so treat reports as sensitive and restrict access. There are no checked-in storage-state files or tokens.

The current repository workflow `.github/workflows/playwright.yml` runs the Java framework. This TypeScript suite can still be run locally with the commands above. Setup dependencies stop downstream projects if health or login preflight fails. API contexts add a unique X-Correlation-ID recorded as a test annotation for server-side investigation.

Design references: [Playwright fixtures](https://playwright.dev/docs/test-fixtures), [projects](https://playwright.dev/docs/test-projects), [web server lifecycle](https://playwright.dev/docs/test-webserver), and [best practices](https://playwright.dev/docs/best-practices).

## Verified baseline

On 19 October 2026, strict TypeScript validation passed and all 42 tests passed on Windows: one setup test, 17 API tests, and eight UI tests in each of Chromium, Firefox, and WebKit. The cross-browser run completed in 53.4 seconds, including application startup. External-mode discovery also confirmed @write tests are excluded without explicit opt-in. The GitHub Actions workflow is provided but has not yet run on a hosted runner.

The initial browser run found the embedded training document was blocked by the default framing header. SecurityConfig now allows SAMEORIGIN only for /automation-frame.html and retains DENY for other routes. An API regression test checks those response headers, and all three browsers exercise the actual frame.