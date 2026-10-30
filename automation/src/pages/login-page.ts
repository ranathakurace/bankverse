import { Page, expect } from '@playwright/test';

export class LoginPage {
  constructor(readonly page: Page) {}
  async goto() {
    await this.page.goto('/');
    await expect(this.page.getByTestId('login-form')).toBeVisible();
  }
  async submit(username: string, password: string) {
    await this.page.getByTestId('login-username').fill(username);
    await this.page.getByTestId('login-password').fill(password);
    await this.page.getByTestId('login-submit').click();
  }
  async expectSignedIn(username: string) {
    await expect(this.page.getByTestId('session-user')).toHaveText(username);
    await expect(this.page.getByTestId('nav-dashboard')).toBeVisible();
  }
}
