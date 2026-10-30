# BankVerse Playwright Java framework

The primary automation framework uses **Java 17, Maven, TestNG, and Playwright Java 1.63.0** for both UI and HTTP API tests. It is a standalone test module; application build dependencies remain separate.

Start with the [full hands-on handbook](docs/HANDBOOK.md). The [offline HTML handbook](docs/handbook.html) includes printable flowcharts. Use the [demo script](docs/DEMO_SCRIPT.md) and [edge-case catalog](docs/EDGE_CASE_CATALOG.md) during training.

## First run on Windows

From this directory:

```powershell
.\scripts\run.ps1 -Suite api
.\scripts\run.ps1 -Suite all -InstallBrowsers
.\scripts\run.ps1 -Suite ui -Browser firefox -InstallBrowsers
.\scripts\run.ps1 -Suite ui -Browser webkit -InstallBrowsers
```

Requires JDK 17+, Maven 3.9+, and network access for initial Maven/browser downloads. No npm command or separately installed Node.js is required; Playwright Java bundles its driver runtime.

Default runs start BankVerse at **http://127.0.0.1:18080**, with a disposable H2 database and synthetic administrator, then stop it. No default PostgreSQL credentials are used.

## Maven commands

For a first test, open `src/test/java/com/bank/qa/tests/ui/FirstPlaywrightTest.java`.
It uses direct Playwright calls with comments for each step: open the login page,
submit an incorrect password, and check the error and signed-out state.
`BaseTest` handles application setup, the disposable administrator, and cleanup.

Run only this example from `automation-java` after installing Chromium:

```powershell
$env:PLAYWRIGHT_BROWSERS_PATH = Join-Path $PWD '.browsers'
mvn test-compile exec:java '-Dexec.mainClass=com.microsoft.playwright.CLI' '-Dexec.args=install chromium'
mvn test '-Dtest=FirstPlaywrightTest' '-Dheadless=false'
```

Omit `-Dheadless=false` to run without a visible browser. A passing run reports
`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

```powershell
mvn test -Papi
mvn test -Pui "-Dbrowser=chromium"
mvn test -Psmoke
mvn test -Pedge
mvn test "-Dtest=CustomersApiTest#eighteenthBirthdayAccepted"
mvn test -Pui "-Dheadless=false" "-Dthreads=1"
```

Use the scripts to install browsers into this module's .browsers directory. Maven profiles select TestNG groups. Class-level parallelism defaults to two threads; each invocation owns its Playwright instance and contexts. Automatic retries are disabled.

## Outputs

- HTML: target/surefire-reports/<browser>/index.html
- XML: target/surefire-reports/<browser>/TEST-TestSuite.xml
- Failed UI trace and screenshot: target/artifacts/<browser>/<unique-test-id>/
- Application startup and server diagnostics: target/application.log

For external QA, copy config/qa.example.properties to ignored config/local.properties, or configure environment variables. Set START_APP=false, URLs, and dedicated employee credentials. Write tests are excluded unless ALLOW_TEST_DATA=true.

The previous TypeScript proof of concept remains under ../automation as historical reference. The repository acceptance CI now runs this Java module.
