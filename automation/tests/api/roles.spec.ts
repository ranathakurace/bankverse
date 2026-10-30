import { randomUUID } from 'node:crypto';
import { test, expect } from '../../src/fixtures/test';
import { expectApiError } from '../../src/assertions/contracts';
import { login } from '../../src/api/auth-client';

test('customer officer can read customers but cannot administer identities or accounts @write', async ({ api, anonymousApi }) => {
  const username = 'pw-officer-' + randomUUID();
  const password = 'PlaywrightOfficer2026!';
  const created = await api.post('/api/v1/admin/employees', { data: {
    username, email: username + '@example.test', password, roles: ['CUSTOMER_OFFICER'],
  } });
  expect(created.status()).toBe(201);
  try {
    const session = await login(anonymousApi, username, password);
    const headers = { Authorization: 'Bearer ' + session.accessToken };
    await expectApiError(await anonymousApi.get('/api/v1/customers/INVALID', { headers }), 400, 'INVALID_CUSTOMER');
    await expectApiError(await anonymousApi.get('/api/v1/accounts/ACC000001', { headers }), 403, 'ACCESS_DENIED');
    await expectApiError(await anonymousApi.get('/api/v1/admin/employees', { headers }), 403, 'ACCESS_DENIED');
  } finally {
    const deactivated = await api.put('/api/v1/admin/employees/' + encodeURIComponent(username) + '/status', { params: { active: false } });
    expect(deactivated.status(), 'Deactivate the synthetic role-test employee').toBe(200);
  }
});
