# CI quality gates

GitHub Actions runs on pull requests, pushes to `main` or `master`, and nightly at 03:17 UTC to refresh cached vulnerability data. Concurrent scans of the same branch or pull request are cancelled so one NVD API key is not rate-limited by overlapping jobs.

| Gate | Command | Failure condition |
| --- | --- | --- |
| Build and automated tests | `mvn verify -Pquality` | Compilation failure, test failure or error |
| JaCoCo coverage | `mvn verify -Pquality` | Executable application code falls below 30% instruction coverage |
| SpotBugs static analysis | `mvn verify -Pquality` | A high-priority SpotBugs finding is detected |
| OWASP dependency scan | `mvn verify -Psecurity -DskipTests` | A dependency with CVSS 7.0 or higher is detected, or both vulnerability data sources are unavailable |

## Local commands

```powershell
mvn verify -Pquality
mvn verify -Psecurity -DskipTests
```

The dependency scan uses the NVD API. In GitHub Actions, the repository secret `NVD_API_KEY` is required and is read only at runtime. The first scan creates a sizeable local vulnerability database; GitHub Actions caches it for later scans. If the live NVD API returns a rate-limit or server-error response, CI retries against OWASP's published NVD data-feed mirror. The mirror is used only for that availability fallback; a confirmed vulnerability finding is never retried or hidden.

Reports are generated under `target/site/jacoco`, `target/spotbugsXml.xml`, and `target/dependency-check-report.*`. GitHub Actions uploads each as a workflow artifact, along with `owasp-live-nvd.log` when the fallback path is evaluated.

No suppression is active by default. If a future finding is confirmed to be a false positive, record its exact scope, justification, expiry date, and tracking ticket in the appropriate suppression file.
