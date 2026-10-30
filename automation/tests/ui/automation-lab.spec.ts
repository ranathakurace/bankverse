import { test, expect } from '../../src/fixtures/test';

test.beforeEach(async ({ signedInPage }) => { await signedInPage.page.getByTestId('nav-dashboard').click(); });

test('dropdowns, multi-selects, radios and checkboxes', async ({ page }) => {
  await page.getByTestId('automation-risk-tier').selectOption('HIGH');
  await expect(page.getByTestId('automation-risk-tier')).toHaveValue('HIGH');
  await page.getByTestId('automation-entitlements').selectOption(['CUSTOMER_READ', 'AUDIT_VIEW']);
  await expect(page.getByTestId('automation-entitlements')).toHaveValues(['CUSTOMER_READ', 'AUDIT_VIEW']);
  await page.getByTestId('automation-route-checker').check();
  await expect(page.getByTestId('automation-route-checker')).toBeChecked();
  await page.getByTestId('automation-override').check();
  await expect(page.getByTestId('automation-override')).toBeChecked();
  await page.getByTestId('automation-show-warning').click();
  await expect(page.getByTestId('automation-alert')).toContainText('checker approval');
});

test('interact with the verification iframe', async ({ page }) => {
  const frame = page.frameLocator('[data-testid="automation-frame"]');
  await frame.getByTestId('frame-verification-method').selectOption({ label: 'Document review' });
  await frame.getByTestId('frame-reference').fill('PW-VERIFICATION');
  await frame.getByTestId('frame-consent').check();
  await frame.getByTestId('frame-verify').click();
  await expect(frame.getByTestId('frame-alert')).toContainText('verification completed');
});

test('handle a popup and return to the main window', async ({ page }) => {
  const pending = page.waitForEvent('popup');
  await page.getByTestId('automation-open-popup').click();
  const popup = await pending;
  try {
    await popup.getByTestId('popup-priority').selectOption({ label: 'High' });
    await popup.getByTestId('popup-reference').fill('PW-CASE');
    await popup.getByTestId('popup-confirm').click();
    await expect(popup.getByTestId('popup-status')).toContainText('Popup action confirmed');
  } finally { await popup.close(); }
  await expect(page.getByTestId('nav-dashboard')).toBeVisible();
});

test('acknowledge a banking error dialog', async ({ page }) => {
  await page.getByTestId('automation-show-error').click();
  await expect(page.getByRole('alertdialog')).toContainText('simulated duplicate transaction');
  await page.getByTestId('banking-error-close').click();
  await expect(page.getByRole('alertdialog')).toBeHidden();
});
