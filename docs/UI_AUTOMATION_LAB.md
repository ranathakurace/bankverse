# UI automation lab

The employee Overview page includes a safe control playground. It does not invoke banking APIs, so Selenium and Playwright tests can freely change controls without affecting data.

## Stable selectors

Use `data-testid` rather than CSS presentation classes:

- `automation-risk-tier`, `automation-region`: native dropdowns.
- `automation-entitlements`: native multi-select. Select two or more options.
- `automation-route-maker`, `automation-route-checker`: radio buttons.
- `automation-mfa`, `automation-override`: checkboxes.
- `automation-limit`: range input; `automation-limit-output` shows the selected value.
- `automation-alert` and `toast-region`: accessible live feedback.
- `automation-show-error` opens `banking-error-modal`; close it with `banking-error-close`.
- `confirmation-modal`: used by real security and transfer changes; its controls are `confirmation-approve` and `confirmation-cancel`.
- `automation-frame`: embedded page. Switch into it, then use `frame-verification-method`, `frame-reference`, and `frame-verify`.
- `automation-open-popup`: opens `/automation-popup.html`. Switch to the new window/page, use `popup-priority`, `popup-reference`, then `popup-confirm`.

## Example intent

Playwright: `page.getByTestId('automation-entitlements').selectOption(['CUSTOMER_READ', 'AUDIT_VIEW'])`.

Selenium: locate `[data-testid='automation-frame']`, switch to the frame, interact, then switch back to default content. For the popup, record current window handles, click `automation-open-popup`, wait for the additional handle, switch, assert `popup-status`, then close and return.

For an enterprise error path through the real UI, submit a valid form with an intentionally unknown reference. The global error dialog presents a user-safe explanation and a generated incident reference while the Response panel preserves the API payload for QA.
