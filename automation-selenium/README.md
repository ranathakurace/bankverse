# BankVerse Selenium enterprise UI automation

This is a standalone Java 17 / Maven / TestNG / Selenium 4 framework. It complements the existing `automation-java` Playwright framework; it does not modify application production code.

## Design

```text
Jenkins (Windows UI agent) → Maven Surefire → TestNG → Page Objects
                                             ├─ Selenium Manager → Chrome / Edge / Firefox
                                             ├─ screenshots + current URL on failure
                                             └─ JUnit XML + HTML-compatible Surefire reports
Managed mode → BankVerse Spring Boot → disposable H2 database
External mode → configured BankVerse URL → dedicated QA account
```

The framework uses explicit waits, Page Objects, stable `data-testid` locators, zero implicit waits, per-test browsers, screenshots on failure, and no automatic test retry. Automatic retry can hide defects and is intentionally excluded.

## Tests included

- Native select dropdown and multi-select dropdown
- Radio button, checkbox and accessible warning alert
- Iframe context switching
- Browser popup-window / window-handle switching
- Enterprise error-dialog acknowledgement

## Safe local run

From this directory, use a terminal with Maven 3.9+ and a browser installed:

```powershell
mvn clean test -Psmoke
mvn test -Pregression
mvn test -Pregression -Dheadless=false -Dbrowser=edge
```

Managed mode is the default. It starts BankVerse at `http://127.0.0.1:18081` with an in-memory H2 database and a disposable `admin` user; it does not use your PostgreSQL data.

Required local tooling: JDK 17+, Maven 3.9+, and Chrome, Edge, or Firefox. Selenium Manager obtains the compatible driver automatically. If your corporate network blocks driver downloads, pre-install the browser/driver on the agent.

## External environment mode

Copy `config/qa.example.properties` to `config/local.properties`. Set a dedicated QA administrator and do not store real credentials in Git. By default, test credentials may also be injected through Jenkins credentials as `TEST_USERNAME` and `TEST_PASSWORD`.

## Jenkins

Create a Pipeline job that points to `automation-selenium/Jenkinsfile`, with a Windows agent labelled `ui-chrome`. The agent must have JDK 17, Maven 3.9+, and Google Chrome installed.

The Jenkinsfile runs managed smoke tests and publishes:

- TestNG JUnit XML to Jenkins Test Results
- `target/surefire-reports/**`
- screenshots/current URL under `target/artifacts/**` when a UI test fails
- `target/application.log`

For an external deployment pipeline, replace the test command with credentials binding and environment values:

```groovy
withCredentials([usernamePassword(credentialsId: 'bankverse-qa-admin', usernameVariable: 'TEST_USERNAME', passwordVariable: 'TEST_PASSWORD')]) {
  bat 'mvn -B -ntp clean test -Psmoke -DstartApp=false -DuiBaseUrl=https://qa.bankverse.example'
}
```

Never run write-capable banking tests against production, and never place credentials in the Jenkinsfile, source, or console output.
