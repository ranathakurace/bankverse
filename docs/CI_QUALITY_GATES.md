# CI quality gates

GitHub Actions runs on pull requests and pushes to `main` or `master`.

| Gate | Command | Failure condition |
| --- | --- | --- |
| Build and automated tests | `mvn verify -Pquality` | Compilation failure, test failure or error |
| JaCoCo coverage | `mvn verify -Pquality` | Executable application code falls below 30% instruction coverage |
| SpotBugs static analysis | `mvn verify -Pquality` | A high-priority SpotBugs finding is detected |
| OWASP dependency scan | `mvn verify -Psecurity -DskipTests` | A dependency with CVSS 7.0 or higher is detected |

## Local commands

```powershell
mvn verify -Pquality
mvn verify -Psecurity -DskipTests
```

The dependency scan downloads vulnerability data. In GitHub Actions, configure the repository secret `NVD_API_KEY` for faster and more reliable NVD access. It is optional but recommended.

Reports are generated under `target/site/jacoco`, `target/spotbugsXml.xml`, and `target/dependency-check-report.*`. GitHub Actions uploads each as a workflow artifact.

No suppression is active by default. If a future finding is confirmed to be a false positive, record its exact scope, justification, expiry date, and tracking ticket in the appropriate suppression file.
