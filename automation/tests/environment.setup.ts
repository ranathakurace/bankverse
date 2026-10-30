import { test, expect } from '@playwright/test';
import { environment as env } from '../config/environment';
import { login } from '../src/api/auth-client';

test('verify environment and provision disposable administrator', async ({ playwright }) => {
  expect(env.username, 'Set TEST_USERNAME for an existing QA environment').not.toBe('');
  expect(env.password, 'Set TEST_PASSWORD for an existing QA environment').not.toBe('');
  const request = await playwright.request.newContext({ baseURL: env.apiBaseUrl, timeout: 15_000 });
  try {
    const health = await request.get('/health');
    expect(health.status()).toBe(200);
    expect(await health.text()).toBe('Banking API is UP');
    const status = await request.get('/auth/bootstrap/status');
    expect(status.status()).toBe(200);
    if ((await status.json()).bootstrapRequired) {
      expect(env.startApp, 'Existing environments must have a provisioned test employee; automatic bootstrap is only allowed in managed mode').toBe(true);
      const bootstrap = await request.post('/auth/bootstrap', { data: {
        username: env.username, password: env.password, email: 'playwright-admin@example.test', roles: ['ADMIN'],
      } });
      expect(bootstrap.ok()).toBe(true);
    }
    const session = await login(request, env.username, env.password);
    expect(session.roles, 'The acceptance suite requires a dedicated ADMIN test employee').toContain('ADMIN');
  } finally { await request.dispose(); }
});
