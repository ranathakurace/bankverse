# BankVerse Playwright Java hands-on handbook

This handbook is for a Java developer or QA engineer learning to test BankVerse through Playwright. It explains setup, framework responsibilities, test execution, debugging, and safe extension. Read ARCHITECTURE.md for the design diagrams, EDGE_CASE_CATALOG.md for scenarios, and DEMO_SCRIPT.md for a presentation walkthrough. The offline handbook.html combines these documents and embeds the flowcharts.

## 1 Prerequisites and workspace

Install a JDK 17 or later and Maven 3.9 or later. Verify both resolve to the intended Java installation:

```powershell
java -version
mvn -version
```

Keep automation-java directly below banking-api. ManagedApplication starts the parent application's Maven build, so moving this module outside the repository requires updating that launcher. The root application still declares Java 11 compatibility; the independent test module deliberately uses Java 17 records, switch expressions, and process APIs.

The first run downloads Maven dependencies and Playwright's Java driver bundle. Browser installation downloads browser builds matching the pinned Java Playwright version. Corporate proxies must allow the configured Maven repository and Playwright CDN. Do not work around TLS problems by disabling certificate verification; configure your proxy/CA through your normal Java and network setup.

No npm installation is needed. Playwright Java supplies its driver runtime. API-only runs require the driver, but no Chromium/Firefox/WebKit installation.

## 2 First successful run

From the repository root, enter automation-java and run:

```powershell
cd automation-java
.\scripts\run.ps1 -Suite api
.\scripts\run.ps1 -Suite all -InstallBrowsers
```

The first command runs HTTP tests only. The second installs Chromium and runs the API plus Chromium UI suite. Each run creates a new temporary application database, provisions its own administrator, and shuts the app down after the suite.

Install and run the other browsers:

```powershell
.\scripts\run.ps1 -Suite ui -Browser firefox -InstallBrowsers
.\scripts\run.ps1 -Suite ui -Browser webkit -InstallBrowsers
```

For a visible demonstration:

```powershell
.\scripts\run.ps1 -Suite smoke -Browser chromium -Headed
```

Use a fresh PowerShell session if execution policy prevents invoking a locally authored script, or run the equivalent Maven commands below under your organization's approved script policy. The framework does not change global execution policy.

Linux/macOS equivalents:

```bash
bash scripts/run.sh api
bash scripts/run.sh all chromium install
bash scripts/run.sh ui firefox install
HEADLESS=false bash scripts/run.sh smoke chromium
```

## 3 What happens to the application

Managed mode binds a new application to 127.0.0.1:18080. A port collision fails the run rather than attaching to another app. ManagedApplication creates a child Maven process in the parent directory and passes datasource/JWT settings only to that process. H2 data disappears when that JVM stops. The existing schema.sql initializes the customer and account sequences.

The framework uses synthetic test data. Default administrator credentials are fixtures for this disposable app, not production credentials. Tests do not connect to the developer's PostgreSQL database.

The readiness wait is bounded at 180 seconds. Startup output is redirected to target/application.log so you can diagnose compilation, dependency, schema, or port failures. A suite failure still triggers application cleanup. If a terminal is force-killed beyond Java shutdown-hook control, inspect the specific test process/port before starting another run; do not indiscriminately terminate Java processes.

## 4 Run suites and individual tests

| Goal | Maven command |
| --- | --- |
| All API and Chromium UI tests | mvn test |
| API only | mvn test -Papi |
| UI only | mvn test -Pui |
| Critical scenarios | mvn test -Psmoke |
| Edge scenarios | mvn test -Pedge |
| Firefox UI | mvn test -Pui "-Dbrowser=firefox" |
| WebKit UI | mvn test -Pui "-Dbrowser=webkit" |
| One class | mvn test "-Dtest=CustomersApiTest" |
| One method | mvn test "-Dtest=CustomersApiTest#eighteenthBirthdayAccepted" |
| Visible UI with one worker | mvn test -Pui "-Dheadless=false" "-Dthreads=1" |
| Compile without execution | mvn test-compile "-DskipTests" |

PowerShell requires quoting the whole -D argument when it contains dots, spaces, or other shell-significant text. The examples use quoted values consistently.

Profiles select groups by union: api, ui, smoke, or edge. A write group marks scenarios that can mutate a persistent QA environment. The exercise group is excluded by default. The lab group marks local HTML skill exercises; these also belong to ui and edge.

Keep tests independent. A smoke run should not rely on a customer created by another test or a previous run. Run a whole class when learning data providers: each input row becomes a separate reported invocation.

## 5 Import into Eclipse or IntelliJ

In Eclipse, use File, Import, Maven, Existing Maven Projects and select banking-api/automation-java. Select JDK 17 for this project. Install TestNG support if your Eclipse distribution does not provide it. Run Maven test from the project or run testng.xml as a TestNG suite. The root banking application can remain a separate imported project.

In IntelliJ, open the module pom.xml as a Maven project and select JDK 17. Prefer a Maven test run configuration for matching CI behavior. A direct TestNG run should use the automation-java directory as its working directory.

Set a breakpoint inside registrationAndSearch, uiRegistrationPersistsThroughApi, or test().api(). Step from test to client/page object to the Playwright call. API bodies remain in memory; avoid printing authentication responses.

The framework's direct-IDE before-method check prevents write-tagged tests from mutating an external environment without opt-in. Use the supplied testng.xml to obtain group filtering and WriteGuard behavior identical to the Maven suite.

## 6 Configuration and existing QA

Configuration precedence is Java system property, environment variable, ignored config/local.properties, then code default. Maven forwards browser/headless defaults to Surefire; select those through the documented -D flags or scripts.

| Java property | Environment variable | Default |
| --- | --- | --- |
| startApp | START_APP | true |
| port | TEST_PORT | 18080 |
| apiBaseUrl | API_BASE_URL | local origin on the test port |
| uiBaseUrl | UI_BASE_URL | local origin on the test port |
| username | TEST_USERNAME | disposable managed administrator |
| password | TEST_PASSWORD | disposable managed password |
| allowTestData | ALLOW_TEST_DATA | false externally; true for managed mode |
| browser | BROWSER | chromium; prefer -Dbrowser |
| headless | HEADLESS | true; prefer -Dheadless or script flag |
| timeoutMs | TIMEOUT_MS | 10000 |

Copy config/qa.example.properties to config/local.properties for local external-QA configuration. That file is ignored by Git. Supply real test credentials through an approved secret store or your session environment. The suite requires a dedicated ADMIN test employee to provision prerequisites and verify privileged workflows.

```powershell
$env:START_APP = "false"
$env:API_BASE_URL = "https://qa.bankverse.example"
$env:UI_BASE_URL = "https://qa.bankverse.example"
$env:TEST_USERNAME = "dedicated-test-admin"
# Set TEST_PASSWORD through your approved secret mechanism.
$env:ALLOW_TEST_DATA = "false"
mvn test -Papi
```

The hostname above is an example, not a configured environment. Replace it with your QA origin. Base URLs must be HTTP(S) origins without embedded credentials, query strings, or path prefixes. The frontend calls relative API URLs, so its host must serve or proxy the backend endpoints.

External mode does not start an app and does not create its first administrator. WriteGuard excludes write-tagged methods while ALLOW_TEST_DATA=false. Logging in still creates tokens, and read-only does not mean that every possible server audit or authentication side effect disappears.

Set ALLOW_TEST_DATA=true only for a QA environment where persistent fixtures are allowed. Customer creation has no supported cleanup endpoint, and financial/audit records should not be deleted through ad hoc SQL. The role test deactivates its employee in finally; the employee row remains. Use an ephemeral QA deployment or your approved reset/retention process.

## 7 Read a UI test from top to bottom

Open tests/ui/CustomersUiTest.java and find uiRegistrationPersistsThroughApi. It performs a real cross-layer check:

1. CustomerData.fresh creates unique synthetic values.
2. test().signedInPage authenticates through HTTP and seeds the correct UI sessionStorage once.
3. CustomersPage.open selects Customers and KYC.
4. CustomersPage.register fills stable test-ID locators, starts a response wait before clicking submit, and verifies the 201 response.
5. The API client retrieves the generated customer number.
6. Contracts.customer checks required response fields and privacy.
7. The test verifies the persisted fullName.
8. BaseTest tears down all resources.

The point of the response wait is synchronization. Do not click, sleep two seconds, and hope registration finished. A response predicate should match the specific endpoint and HTTP method. Web-first assertions wait for the DOM condition automatically.

For login/logout behavior, use a blank test().page() plus LoginPage. Do not call signedInPage again after logout, because that helper intentionally logs the test back in.

## 8 Read an API test

Open tests/api/LedgerApiTest.java and repeatedDepositDoesNotDuplicateMoney. The fixture first completes an isolated customer/KYC/account flow through OnboardingFlow. It then deposits 125.50 using a UUID idempotency key, repeats the same request/key, checks the same transaction reference, and verifies the account balance is still 125.50.

Asserting HTTP 201 alone would miss a duplicate deposit. Assert a business side effect: balance, transaction reference, or a relevant history count. Use BigDecimal.compareTo for currency assertions so 125.5 and 125.50 compare numerically without floating-point rounding.

The reusedKeyReplaysOriginalAmount test documents current application behavior: a same-operation replay returns the original result even if the second payload changes the amount. It returns HTTP 201 with the original amount and does not add funds. The backend currently does not reject same-operation payload mismatch. A different operation using the same key returns 409. This distinction is explained in the edge catalog; it is not a guarantee of strict payload-fingerprint validation.

## 9 Add your first test

Start with unknown-email lookup in CustomersApiTest. Use the existing API client and contract helper:

```java
@Test(groups = {"api", "edge"})
public void unknownCustomerEmailReturns404() {
  var customers = new CustomersClient(test().api());
  var response = customers.byEmail(CustomerData.fresh().email());
  Contracts.error(response, 404, "CUSTOMER_NOT_FOUND");
}
```

Add imports for CustomersClient, CustomerData, Contracts, and Test. Save under the module's src/test/java/com/bank/qa/tests/api package. A class name must end in Test for Surefire's default discovery.

Run that method with mvn test "-Dtest=YourClassTest#unknownCustomerEmailReturns404". Verify it fails if you deliberately change the expected status, then restore the correct assertion. This proves the test can detect a wrong outcome.

For a mutation, include write in its groups. WriteGuard recognizes method-level groups; place tags on each method rather than only on the class. Do not use dependsOnMethods to chain customer creation and lookup as separate tests. Build prerequisites within the scenario or a reusable flow.

## 10 Add a new module cleanly

For cards, loans, payments, or transfers, add a domain client under api. Add a page object only for a UI surface that exists. Put reusable prerequisite sequences into flows. Generate new data per test and use explicit response contracts.

Keep endpoint strings inside clients rather than duplicating them across tests. Keep CSS presentation details out of test logic. Keep business assertions visible in tests. A page object may wait until a screen is usable, but it should not silently approve a business workflow or catch failures just to continue.

For a transfer test, create two independent active accounts, fund one through the API, initiate the transfer, verify both balances and references, then check replay behavior. Use a new idempotency key per logically new transfer. Do not automatically retry a failed transfer with a new key, because the first request may already have committed.

## 11 Learn Playwright interactively

Use the Inspector:

```powershell
$env:PWDEBUG = "1"
mvn test "-Dtest=AutomationLabTest#verificationInsideFrame" "-Dheadless=false" "-Dthreads=1"
Remove-Item Env:PWDEBUG
```

Set a breakpoint and call page.pause temporarily when practicing. Remove pauses before committing an automated test.

Generate Java locator ideas with Codegen against an already running QA application:

```powershell
$env:PLAYWRIGHT_BROWSERS_PATH = "$PWD/.browsers"
mvn test-compile exec:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=codegen --target=java http://localhost:8080"
```

Codegen does not start the app. Use the actual QA address and interact only with test data. Generated code is a starting point: move selectors into page objects, replace fixed data, and add business assertions.

## 12 Diagnose a failed test

Read the first failing assertion in target/surefire-reports/<browser>/index.html or the XML report. Separate setup failures from test failures. A bootstrap/login failure usually concerns the environment or credentials; a Playwright timeout concerns an unmet locator/action condition; a contract failure identifies unexpected HTTP/body behavior.

Inspect target/application.log for backend exceptions. Authenticated API tests include a correlation ID in their request headers and in failure output. Use that ID for the corresponding API calls when server logs include correlation metadata.

Failed UI tests retain failure.png and trace.zip under target/artifacts/<browser>/<unique-id>. Use the trace to inspect action order, DOM snapshots, network timing, and screenshots.

```powershell
mvn test-compile exec:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=show-trace target/artifacts/chromium/ACTUAL-TEST-ID/trace.zip"
```

Replace ACTUAL-TEST-ID with an existing directory. Java browser-context traces contain Playwright calls and snapshots; they do not record TestNG assertions with the same rich runner integration as TypeScript Playwright Test. Read the TestNG report for the assertion itself.

Traces can contain access tokens, form values, and network data. Treat screenshots, traces, logs, and reports as restricted QA artifacts. The framework does not claim complete trace redaction. API traces are not enabled.

## 13 Practice failure evidence deliberately

An intentionally failing exercise is provided outside the default groups:

```powershell
mvn test "-Dtest=FailureEvidenceExerciseTest" "-Dgroups=exercise" "-DexcludedGroups=" "-DresultsLabel=evidence-exercise"
```

The expected result is one failed UI test and a non-zero Maven exit code. This is a learning exercise, not a passing acceptance run. Open its screenshot and trace, identify the deliberately wrong username expectation, and return to mvn test for normal coverage. The default suite and CI exclude exercise.

## 14 Edge-case learning route

Complete these labs in order: health/correlation; login and stale bearer; mandatory fields and data providers; exact eighteenth birthday; duplicate identity fields; UI/API persistence; iframe; popup; intercepted service failure; synthetic upload/download/native dialog; deposit replay and balances; role separation; failure evidence.

For each lab, predict the result, run one method, inspect its source, change one input, explain the observed result, and restore or extend the test. Record whether it is real application coverage, mocked UI recovery coverage, or a local tool exercise.

EDGE_CASE_CATALOG.md lists implemented scenarios, method names, expected outcomes, and additional exercises. Do not present proposed concurrency/security cases as already automated.

## 15 CI and operational limits

The repository's playwright.yml now runs Java tests on Ubuntu in a three-browser matrix. Chromium runs API plus UI tests. Firefox and WebKit run UI tests. Each job installs OS browser dependencies and uploads Surefire reports, artifacts, and startup logs. There is no separately installed Node.js requirement.

The workflow has not been verified on a hosted runner until your repository executes it. Browser/OS differences may require environment-specific investigation. Increase job timeout only after checking the cause.

Local runs overwrite reports for the same browser and application.log; failed evidence directories have unique IDs. Copy a run's artifacts before rerunning if you need a historical audit trail. CI retains per-job artifacts for seven days.

This framework is an extensible acceptance baseline. It does not yet automate every banking module, every role permission, high-volume performance, full accessibility auditing, strict payload-fingerprint idempotency, or concurrent financial races. These belong in a broader quality strategy with application unit/integration tests and an isolated PostgreSQL environment.

## 16 Maintain the handbook and check the write guard

The handbook sources are Markdown files in docs plus two SVG diagrams and editable Mermaid sources. Rebuild the self-contained HTML after editing those sources:

```powershell
python scripts/build_handbook.py
```

Python is needed only to regenerate the handbook, not to run Java tests. The optional HandbookPreview Java utility checks local links, embedded diagrams, and viewport overflow and writes preview screenshots under target/docs-preview.

A separate opt-in infrastructure probe verifies that WriteGuard excludes a write method in external mode. It does not start or contact an application:

```powershell
mvn test "-Dtest=ExternalWriteGuardTest" "-Dgroups=guard" "-DexcludedGroups=" "-DstartApp=false" "-Dusername=guard-probe" "-Dpassword=guard-probe-not-a-real-credential" "-DresultsLabel=external-guard"
```

Expected result: one read-only probe passes; the deliberately failing write method is removed before execution. The strings above are dummy configuration values and are never used for login. The probe belongs to exercise and is excluded from the normal suite.

Use -DresultsLabel with a descriptive run name to retain a separate report directory, such as acceptance-chromium. Failure evidence already uses unique test IDs; do not post evidence publicly.