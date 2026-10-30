# Demo script for the Java UI and API framework

Use this as a 15-minute walkthrough. Run browser installation before the meeting. The main story is a single Java acceptance framework with separate UI/API abstractions, isolated data, and visible business assertions.

## Before the demo

Open docs/handbook.html, the architecture diagram, and your IDE. Verify JDK 17 and Maven. Run the suite once so dependency downloads do not consume presentation time. Keep all targets on the disposable managed environment. Close other acceptance runs that own port 18080.

Have these source files ready: pom.xml, BaseTest, TestSession, CustomersClient, CustomersPage, CustomerData, CustomersUiTest, LedgerApiTest, and the TestNG HTML report.

## Minutes 0 to 2 Explain the architecture

Say: "We use Playwright Java for browser and HTTP automation, TestNG for lifecycle and data-driven tests, and Maven Surefire for execution and reports. UI tests use page objects. API tests use domain clients. Both share configuration, isolated data factories, contracts, and evidence handling."

Point to the diagram from entry point to per-test session, then split into UI and API paths. Explain that APIRequestContext does not require launching a browser.

Say: "Playwright Java objects remain on the thread that created them. Each invocation owns its session, and we run independent classes on two threads. This avoids sharing a Page or API context across workers."

## Minutes 2 to 4 Show execution lifecycle

Point to the execution flowchart. Explain managed versus external mode. Managed mode starts the real application with H2 on a separate local port, verifies health, creates a disposable administrator, and stops owned processes afterward. External mode expects a provisioned QA employee and excludes write tests without explicit opt-in.

Show RunConfig and WriteGuard. Explain that configuration is validated before testing and secrets are ignored by Git.

## Minutes 4 to 6 Run an API edge case

Run:

```powershell
mvn test "-Dtest=CustomersApiTest#invalidCustomerFields"
```

Open the DataProvider while the run executes. Explain why name length 101, invalid phone prefix, malformed PAN, and future DOB produce separate reported cases. Show that assertions check structured error codes rather than only success/failure.

Open Contracts.customer and point out privacy assertions excluding PAN/Aadhaar/contact fields from customer responses.

## Minutes 6 to 9 Demonstrate the hybrid scenario

Run:

```powershell
mvn test "-Dtest=CustomersUiTest#uiRegistrationPersistsThroughApi" "-Dheadless=false" "-Dthreads=1"
```

Walk through fresh data, API authentication, page-object navigation, form submission, the response wait, and the final API lookup. Explain why a success toast alone would not prove database persistence.

Say: "We seed sessionStorage once for scenarios whose purpose is customer behavior. Separate tests use the real login form to validate authentication and logout. A new browser context has no prior session."

## Minutes 9 to 11 Show Playwright-specific interactions

Run AutomationLabTest in headed mode or open its passing report. Point out selectOption, check, frameLocator, waitForPopup, and custom modal assertions.

Open serviceFailureShowsRecoverableDialog. Explain that the 503 is fulfilled at the browser network boundary and verifies UI recovery. It is not a backend availability test. Mention that local upload/download/dialog exercises are marked as tool labs.

## Minutes 11 to 13 Explain financial assertions

Open repeatedDepositDoesNotDuplicateMoney. Show that the same idempotency key returns the same reference and that the balance increases once. Open idempotencyKeyCannotChangeOperation and explain the 409 branch.

State the current implementation precisely: "A replay of the same operation uses the original result even if the second amount differs. The test verifies that the changed amount is not applied. Strict payload-mismatch rejection is a separate product decision."

Explain that OnboardingFlow creates fresh KYC-approved accounts for each financial scenario, avoiding dependence on previous test runs.

## Minutes 13 to 15 Show evidence and CI

Open target/surefire-reports/chromium/index.html. Point out data-provider invocations, method names, and failure details. If demonstrating failure capture, use the explicitly excluded FailureEvidenceExerciseTest rather than editing passing business expectations during the presentation.

Show the CI matrix: Chromium executes API plus UI; Firefox and WebKit execute UI. Each job has its own disposable database and uploads reports. Explain that release confidence also requires external-mode execution against isolated PostgreSQL QA.

End by showing where to add a test: scenario in tests, endpoint action in api, UI action in pages, prerequisites in flows, reusable response checks in assertions, and unique data in data.

## Questions you may receive

| Question | Suggested explanation |
| --- | --- |
| Why Java? | It matches the team's Java/Maven skill set and existing application tooling while using Playwright for both HTTP and UI. |
| Why TestNG? | It supplies groups, lifecycle hooks, DataProvider, parallel execution, and familiar HTML/XML outputs. |
| Is there a separate REST library? | No. HTTP requests use Playwright APIRequestContext. |
| Why separate Maven module? | Browser/testing dependencies do not become application runtime dependencies. |
| Why no automatic retries? | We want failures visible, especially for state-changing banking operations. A retry with a new key can duplicate a committed request. |
| Does a passing suite prove every module works? | No. The catalog lists implemented coverage and the backlog separately. |
| Can we use PostgreSQL? | Yes, point external mode at an isolated deployed QA instance. The default managed H2 run is faster but does not prove database parity. |
| Can we run TestNG methods in parallel? | The implementation owns per-invocation sessions; the validated configuration is class-level parallelism. Revalidate any scheduling change and keep Playwright calls on their owner thread. |
| Are traces safe to share publicly? | No. They can contain network/session data and should remain restricted QA artifacts. |
| Where is Playwright Test's reporter? | That runner is TypeScript-specific. Java uses TestNG/Surefire reports and browser-context trace capture. |
