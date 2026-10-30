import { test, expect } from '../../src/fixtures/test';
import { expectApiError } from '../../src/assertions/contracts';
import { environment as env } from '../../config/environment';

test('health endpoint propagates correlation ID @smoke', async ({ anonymousApi }) => {
  const response = await anonymousApi.get('/health', { headers: { 'X-Correlation-ID': 'playwright-health-check' } });
  expect(response.status()).toBe(200);
  expect(await response.text()).toBe('Banking API is UP');
  expect(response.headers()['x-correlation-id']).toBe('playwright-health-check');
});

test('valid employee login returns an access token @smoke', async ({ anonymousApi }) => {
  const response = await anonymousApi.post('/auth/login', { data: { username: env.username, password: env.password } });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(typeof body.accessToken).toBe('string');
  expect(body.accessToken.length).toBeGreaterThan(0);
  expect(body.roles).toContain('ADMIN');
});

test('invalid password is rejected', async ({ anonymousApi }) => {
  const response = await anonymousApi.post('/auth/login', { data: { username: env.username, password: 'IncorrectTestPassword123!' } });
  await expectApiError(response, 401, 'AUTHENTICATION_FAILED');
});

test('protected endpoints require authentication @smoke', async ({ anonymousApi }) => {
  await expectApiError(await anonymousApi.get('/api/v1/customers/CUST000100'), 401, 'AUTHENTICATION_REQUIRED');
});

test('invalid bearer token is rejected', async ({ anonymousApi }) => {
  const response = await anonymousApi.get('/api/v1/customers/CUST000100', { headers: { Authorization: 'Bearer invalid-token' } });
  await expectApiError(response, 401, 'INVALID_ACCESS_TOKEN');
});

test('only the embedded training page permits same-origin framing', async ({ anonymousApi }) => {
  const frame = await anonymousApi.get('/automation-frame.html');
  expect(frame.status()).toBe(200);
  expect(frame.headers()['x-frame-options']).toBe('SAMEORIGIN');
  for (const path of ['/', '/health', '/automation-popup.html']) {
    const response = await anonymousApi.get(path);
    expect(response.status()).toBe(200);
    expect(response.headers()['x-frame-options']).toBe('DENY');
  }
});
