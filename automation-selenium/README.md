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

## Regression hardening and readiness gate

The regression suite contains six focused scenarios across two classes: risk selection,
multiselect clearing, checker approval warning, iframe verification, popup confirmation,
and error-dialog acknowledgement. Each method receives a fresh browser. Class-level
parallelism keeps a class instance on one worker; method-level parallelism is not supported
by the instance-field driver lifecycle.

Run from `automation-selenium` with Java, Maven and a matching browser/driver on PATH:

```bash
mvn -B -ntp clean test -Pregression -Dui.threads=1
mvn -B -ntp clean test -Pregression -Dui.threads=2
java ../ci/jenkins/VerifyUiResults.java target 6
```

Use identical source, browser and machine for timing comparisons. Keep the reports from
each run because `clean` removes previous output. Repeat and alternate one/two-thread
runs; compare medians and report setup/compilation/cache effects. A small suite does not
establish production-scale throughput.

`target/browser-sessions.tsv` records OPEN/CLOSED, class/method, worker ID, session ID and
UTC timestamp. The gate requires at least six unique passing test methods, no failed or
skipped TestNG configuration methods, one unique browser session per test, and matching
cleanup on the owning worker. A successful quit call is recorded as CLOSED; this does not
independently prove the absence of every orphan operating-system process.

Failure evidence remains in `target/artifacts/<class>-<method>-<UUID>/`, with `failure.png`,
`url.txt` and `execution.txt`. Screenshot errors are surfaced instead of silently ignored.
Controlled failure exercises belong in an isolated copy or Replay, and must be restored;
never commit intentionally failing expectations to the release branch.

`ci/jenkins/Jenkinsfile.regression` is the proposed no-socket pipeline for the current Linux
learning environment. It adds the UI gate to existing backend test/JaCoCo/SpotBugs gates
and archives lifecycle evidence. It requires the committed hardening changes on GitHub
before the Jenkins job is switched to it. The existing root Docker-agent pipeline is separate.
The gate provides automated readiness evidence; it is not deployment approval and does
not replace security, business acceptance, performance or cross-browser validation.

## CI execution dashboard

`ci/jenkins/GenerateCiDashboard.java` generates a build-specific, script-free HTML report
at `ci-dashboard/index.html`. It reads backend/UI JUnit XML, JaCoCo XML, SpotBugs XML,
browser lifecycle TSV, readiness evidence and PNG screenshots. No extra runtime or
Jenkins plugin is required beyond the existing JDK. External CSS is archived alongside
the HTML so the report works with Jenkins's default content-security policy.

The updated `ci/jenkins/Jenkinsfile.regression` generates and archives it in pipeline
`post { always { ... } }`, including failed test builds. The checkout starts with
`deleteDir()` to prevent prior workspace reports from appearing in a new build. Successful
backend and UI gate execution is recorded in build-local environment flags. The dashboard
shows unavailable evidence instead of treating missing reports as zero failures. If source
checkout fails before the generator is available, Jenkins logs why no dashboard was produced.

After adopting this pipeline, open:

```text
<build URL>/artifact/ci-dashboard/index.html
```

`summary.txt` contains a plain-text summary. The dashboard links to archived backend/UI
reports, coverage, SpotBugs findings, application logs, lifecycle records and failure
screenshots from the same build. A passing build normally has no screenshots. The
AUTOMATED GATES PASSED banner requires a successful Jenkins result, confirmed gate
execution, passing tests without skips, instruction coverage above the configured minimum,
zero configured SpotBugs findings/errors, and complete browser lifecycle evidence.
It does not grant deployment approval or confirm security/business acceptance.

Parallel metrics show worker count, distinct opened/closed sessions, peak concurrent
sessions and measured browser-session span. These are not the Maven duration and do not
establish a speedup from one build; use repeated sequential/parallel trials for comparison.

To generate a local preview from archived evidence, place the archived report directories
and matching pom.xml under a snapshot root, copy the generator/CSS under ci/jenkins there,
and run `java ci/jenkins/GenerateCiDashboard.java .`. Without build-local result and gate
flags it intentionally reports INCOMPLETE rather than assuming success.
