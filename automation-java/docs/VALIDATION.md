# Validation record

Validated on Windows with JDK 17 and Maven on 19 October 2026.

| Run | Result |
| --- | --- |
| Maven Java compilation | Passed |
| Default API plus Chromium UI suite | 61 passed, zero failed, zero skipped |
| Firefox UI suite | 16 passed, zero failed, zero skipped |
| WebKit UI suite | 16 passed, zero failed, zero skipped |
| Combined acceptance invocations | 93 passed |
| PowerShell smoke entry point | 7 passed |
| External write guard probe | Passed; one write method excluded and one read-only probe executed without contacting an application |
| Failure evidence exercise | Expected single failure, nonzero exit, screenshot and trace verified in a separate report directory |
| Offline handbook QA | Desktop/mobile overflow and chapter-link checks passed; both embedded diagrams visually inspected |
| Managed application lifecycle | Automatic startup and process cleanup completed for each run |
| Existing application code | No additional business-code changes were required for the Java migration |

The 61-invocation default run consists of 45 API invocations and 16 UI invocations. DataProvider rows count separately. Three UI cases use synthetic local HTML to teach uploads, downloads, and native dialogs.

The first expanded run exposed test-expectation differences from the current application contract. Pending-KYC account creation returns 400 INVALID_CUSTOMER. Same-operation idempotency replay returns the original result with HTTP 201 even if the second amount differs. Final tests document those behaviors and separately verify a 409 when the key is reused across deposit and withdrawal.

The final full Chromium report is under target/surefire-reports/acceptance-chromium. Firefox and WebKit reports are under their corresponding browser folders. The smoke report is in chromium; the intentional failure is in evidence-exercise; the write-guard probe is in external-guard. These are local results. The GitHub Actions workflow is configured but has not run on a hosted runner in this session. External PostgreSQL coverage, complete role matrices, and concurrency races remain future coverage.
