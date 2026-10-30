# Edge-case catalog and practice route

The default suite contains 45 API invocations and 16 UI invocations per browser. Data-provider rows count separately. Local tool labs are included in UI execution and clearly labeled below. The intentionally failing exercise is excluded by default.

## Implemented API cases

| ID | Source and method | Edge or business check | Expected result |
| --- | --- | --- | --- |
| API-01 | HealthAuthTest.healthAndCorrelation | Health endpoint and caller correlation header | 200, exact health text, matching X-Correlation-ID |
| API-02 | HealthAuthTest.validLogin | Valid ADMIN credentials | Access token and ADMIN role |
| API-03 | HealthAuthTest.invalidPassword | Incorrect password | 401 AUTHENTICATION_FAILED |
| API-04 | HealthAuthTest.missingAuthentication | Protected customer endpoint without token | 401 AUTHENTICATION_REQUIRED |
| API-05 | HealthAuthTest.malformedBearer | Malformed bearer token | 401 INVALID_ACCESS_TOKEN |
| API-06 | HealthAuthTest.staleBearerDoesNotBlockLogin | Old Authorization header on a new login | Successful login |
| API-07 | HealthAuthTest.framingPolicy | Embedded training page versus main page | SAMEORIGIN on frame; DENY on main |
| API-08 | CustomersApiTest.registrationAndSearch | Fresh registration, number/PAN/email/phone lookups, case/whitespace normalization, response privacy | 201 then 200; same customer number; no PAN/Aadhaar/phone/email in response |
| API-09 | CustomersApiTest.invalidCustomerFields | Eleven rows: blank/short/long/nonalphabetic name, bad/oversized email, short/invalid-prefix phone, bad PAN, bad Aadhaar, future DOB | 400 INVALID_CUSTOMER |
| API-10 | CustomersApiTest.omittedMandatoryField | Six rows omitting each mandatory field | 400 INVALID_CUSTOMER |
| API-11 | CustomersApiTest.eighteenthBirthdayAccepted | DOB exactly 18 years before the application day | 201 |
| API-12 | CustomersApiTest.oneDayUnderEighteenRejected | DOB one day younger than 18 | 400 INVALID_CUSTOMER |
| API-13 | CustomersApiTest.maximumNameAccepted | Exactly 100 alphabetic characters | 201 |
| API-14 | CustomersApiTest.duplicateIdentityRejected | Four rows independently duplicating PAN/email/phone/Aadhaar | 409 CUSTOMER_ALREADY_EXISTS |
| API-15 | CustomersApiTest.invalidReference | Malformed customer number | 400 INVALID_CUSTOMER |
| API-16 | CustomersApiTest.unknownEmail | Random valid email that does not exist | 404 CUSTOMER_NOT_FOUND |
| API-17 | CustomersApiTest.malformedJson | Truncated JSON document | 400 INVALID_REQUEST |
| API-18 | LedgerApiTest.repeatedDepositDoesNotDuplicateMoney | Same deposit and same idempotency key twice | Same transaction reference; balance increases once |
| API-19 | LedgerApiTest.reusedKeyReplaysOriginalAmount | Same operation/key with changed second amount | Current policy: original transaction/amount returned with 201; balance unchanged |
| API-20 | LedgerApiTest.idempotencyKeyCannotChangeOperation | Deposit key reused for withdrawal | 409 TRANSACTION_INVALID_STATE; balance unchanged |
| API-21 | LedgerApiTest.insufficientFundsPreserveBalance | Withdraw from zero-balance active account | 409 TRANSACTION_INVALID_STATE; zero balance remains |
| API-22 | LedgerApiTest.rejectInvalidAmounts | Three rows: zero, negative, more than two decimals | 400 INVALID_TRANSACTION |
| API-23 | LedgerApiTest.freezePreventsDeposit | Deposit into a frozen account | 409 TRANSACTION_INVALID_STATE |
| API-24 | LedgerApiTest.pendingKycCannotOpenAccount | Open account before customer activation/KYC | 400 INVALID_CUSTOMER |
| API-25 | RoleAccessTest.customerOfficerCannotOperateAccountsOrAdministerStaff | A fresh CUSTOMER_OFFICER reads customer domain but accesses accounts/admin | Customer validation reachable; protected modules return 403 ACCESS_DENIED; employee deactivated afterward |

API-09 expands into 11 tests, API-10 into six, API-14 into four, and API-22 into three. The other 21 cases execute once: 45 API invocations in total.

DOB boundaries use LocalDate.now in the test JVM. Keep the QA application and test runner on the same business date/timezone for those cases; across-midnight or differently zoned deployments require an injected business clock. Managed mode runs them on the same host.

## Implemented UI cases

| ID | Source and method | Skill or scenario | Coverage type |
| --- | --- | --- | --- |
| UI-01 | LoginUiTest.signInAndLogoutStayEffectiveAfterReload | Real login/logout, cleared sessionStorage, reload | Real application |
| UI-02 | LoginUiTest.invalidCredentialsShowFeedback | Invalid-login error, form remains available | Real application |
| UI-03 | LoginUiTest.keyboardEnterSubmitsLogin | Keyboard submission | Real application |
| UI-04 | LoginUiTest.authenticationSurvivesReloadButNotNewContext | Session reload persistence and BrowserContext isolation | Real application |
| UI-05 | CustomersUiTest.uiRegistrationPersistsThroughApi | Create in UI and verify via API | Real hybrid scenario |
| UI-06 | CustomersUiTest.apiCreatedCustomerFoundInUi | API setup, UI lookup and rendered result | Real hybrid scenario |
| UI-07 | CustomersUiTest.duplicateRegistrationShowsBusinessError | Real backend conflict and UI error modal | Real application |
| UI-08 | CustomersUiTest.requiredFieldBlocksSubmission | Native required-field validity and absence of result | Real UI validation |
| UI-09 | CustomersUiTest.serviceFailureShowsRecoverableDialog | route.fulfill with synthetic 503, feedback and modal dismissal | Mocked UI recovery |
| UI-10 | AutomationLabTest.selectsRadiosAndCheckboxes | Single/multi-select, radio, checkbox, live feedback | Existing app training lab |
| UI-11 | AutomationLabTest.verificationInsideFrame | FrameLocator and embedded controls | Existing app training lab |
| UI-12 | AutomationLabTest.popupLifecycle | Register popup wait before click, use/close popup, return | Existing app training lab |
| UI-13 | AutomationLabTest.customBankingErrorDialog | Accessible custom error modal | Existing app training lab |
| UI-14 | ToolPracticeTest.uploadSyntheticDocumentInLocalLab | FilePayload upload without external fixture file | Local HTML tool lab |
| UI-15 | ToolPracticeTest.nativeConfirmDialogInLocalLab | Register dialog listener before click, dismiss confirm | Local HTML tool lab |
| UI-16 | ToolPracticeTest.downloadCsvAndInspectBytesInLocalLab | waitForDownload, filename and content | Local HTML tool lab |

The local HTML upload/download/dialog labs do not demonstrate application KYC upload or bank-statement export. They deliberately use page.setContent to learn Playwright APIs without inventing unsupported application features.

## Exercises to perform hands-on

| Stage | Exercise | How to run | What to explain |
| --- | --- | --- | --- |
| 1 | Health and correlation | mvn test "-Dtest=HealthAuthTest#healthAndCorrelation" | APIRequestContext can work without a browser |
| 2 | Customer boundaries | mvn test "-Dtest=CustomersApiTest#invalidCustomerFields" | One DataProvider row produces one invocation |
| 3 | Birthday edge | mvn test "-Dtest=CustomersApiTest#eighteenthBirthdayAccepted" | Why exact boundary values matter |
| 4 | Real hybrid check | mvn test "-Dtest=CustomersUiTest#uiRegistrationPersistsThroughApi" | UI success plus persisted API state |
| 5 | Frame and popup | mvn test "-Dtest=AutomationLabTest" "-Dheadless=false" "-Dthreads=1" | Frame context versus separate Page |
| 6 | UI service failure | mvn test "-Dtest=CustomersUiTest#serviceFailureShowsRecoverableDialog" | Network mocking tests UI recovery, not backend availability |
| 7 | File and dialog APIs | mvn test "-Dtest=ToolPracticeTest" | Synthetic local lab versus banking coverage |
| 8 | Money replay | mvn test "-Dtest=LedgerApiTest#repeatedDepositDoesNotDuplicateMoney" | Why reference and balance assertions are necessary |
| 9 | Role boundary | mvn test "-Dtest=RoleAccessTest" | A hidden menu is not sufficient authorization proof |
| 10 | Evidence practice | Select FailureEvidenceExerciseTest with exercise group and no exclusions | How a genuine failing run produces screenshots/trace and non-zero exit |

## Additional cases to implement next

These are proposed exercises, not executed coverage. Confirm the product contract before deciding expected statuses.

| Area | Proposed exercise | Design guidance |
| --- | --- | --- |
| Financial concurrency | Two simultaneous withdrawals against limited balance | Separate Playwright instance on each thread; check final balance and ledger invariants on PostgreSQL |
| Idempotency race | Concurrent identical keys | Verify one business effect; document how duplicate in-flight requests are handled |
| Strict idempotency | Same key, different account or amount | Current implementation replays same-operation result; agree on payload fingerprint/conflict policy before tightening assertions |
| Currency | Cross-currency transfer | Check documented rejection and unchanged source/destination balances |
| Boundary money | Smallest permitted amount, large amount, decimal precision | Use BigDecimal, not double; agree on maximum monetary limits |
| Workflow | Closing nonzero account, repeated freeze/unfreeze, invalid KYC order | Assert status, structured code, and unchanged state |
| Authentication | Expired JWT, tampered signature, refresh-token rotation/reuse | Obtain controlled fixtures without storing signing keys or tokens in reports |
| Authorization | Full banking role-by-endpoint matrix | Create least-privilege users and verify API restrictions independently of menu visibility |
| Customer data | Duplicate normalized email/PAN, Unicode names, leap-day DOB | Establish intended Unicode and age policy before assuming support |
| UI resilience | Delayed response, request abort, duplicate-click prevention | Use routing/events; avoid fixed sleeps and production backend disruption |
| UI accessibility | Keyboard focus on custom dialogs and aria announcements | Define accessible behavior and add focused assertions |
| Browser session | Expiry during navigation, browser back after logout | Distinguish cached DOM appearance from successful protected API access |
| Negative HTTP | Missing idempotency header, wrong content type, unsupported method | Check application contract; framework errors may differ from domain validation errors |
| Report retention | CI artifact access and expiration | Verify repository permissions and retention policy in the actual hosted environment |
