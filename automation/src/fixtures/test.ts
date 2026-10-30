import { test as base, APIRequestContext } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { environment as env } from '../../config/environment';
import { login, Session } from '../api/auth-client';
import { CustomersClient, CustomerRequest } from '../api/customers-client';
import { customerData } from '../data/customer-factory';
import { LoginPage } from '../pages/login-page';
import { CustomersPage } from '../pages/customers-page';

type Fixtures = {
  anonymousApi: APIRequestContext;
  api: APIRequestContext;
  customers: CustomersClient;
  customer: CustomerRequest;
  loginPage: LoginPage;
  customersPage: CustomersPage;
  signedInPage: CustomersPage;
};
type WorkerFixtures = { session: Session };

export const test = base.extend<Fixtures, WorkerFixtures>({
  session: [async ({ playwright }, use) => {
    const request = await playwright.request.newContext({ baseURL: env.apiBaseUrl });
    try { await use(await login(request, env.username, env.password)); }
    finally { await request.dispose(); }
  }, { scope: 'worker' }],
  anonymousApi: async ({ playwright }, use) => {
    const request = await playwright.request.newContext({ baseURL: env.apiBaseUrl, timeout: 15_000 });
    try { await use(request); } finally { await request.dispose(); }
  },
  api: async ({ playwright, session }, use, testInfo) => {
    const correlationId = `pw-${randomUUID()}`;
    const request = await playwright.request.newContext({
      baseURL: env.apiBaseUrl, timeout: 15_000,
      extraHTTPHeaders: { Authorization: `Bearer ${session.accessToken}`, 'X-Correlation-ID': correlationId },
    });
    testInfo.annotations.push({ type: 'correlation-id', description: correlationId });
    try { await use(request); } finally { await request.dispose(); }
  },
  customers: async ({ api }, use) => { await use(new CustomersClient(api)); },
  customer: async ({}, use) => { await use(customerData()); },
  loginPage: async ({ page }, use) => { await use(new LoginPage(page)); },
  customersPage: async ({ page }, use) => { await use(new CustomersPage(page)); },
  signedInPage: async ({ page, session, customersPage }, use) => {
    // BankVerse stores auth in sessionStorage, which storageState does not persist.
    await page.addInitScript(({ session, origin }) => {
      if (location.origin !== origin) return;
      sessionStorage.setItem('bankverse.accessToken', session.accessToken);
      sessionStorage.setItem('bankverse.user', session.username);
      sessionStorage.setItem('bankverse.roles', JSON.stringify(session.roles));
      sessionStorage.setItem('bankverse.portal', 'EMPLOYEE');
    }, { session, origin: env.uiBaseUrl });
    await page.goto('/');
    await new LoginPage(page).expectSignedIn(session.username);
    await use(customersPage);
  },
});
export { expect } from '@playwright/test';
