import { test, expect } from '../../src/fixtures/test';
import { environment as env } from '../../config/environment';

test('employee can sign in and sign out @smoke', async ({ loginPage, page }) => {
  await loginPage.goto();
  await loginPage.submit(env.username, env.password);
  await loginPage.expectSignedIn(env.username);
  await page.getByTestId('logout').click();
  await expect(page.getByTestId('login-form')).toBeVisible();
  expect(await page.evaluate(() => sessionStorage.getItem('bankverse.accessToken'))).toBeNull();
  await page.reload();
  await expect(page.getByTestId('login-form')).toBeVisible();
});

test('invalid credentials show a helpful message', async ({ loginPage, page }) => {
  await loginPage.goto();
  await loginPage.submit(env.username, 'IncorrectTestPassword123!');
  await expect(page.getByRole('alert').filter({ hasText: 'Invalid credentials.' })).toBeVisible();
  await expect(page.getByTestId('login-form')).toBeVisible();
});
