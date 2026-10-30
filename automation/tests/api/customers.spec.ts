import { test, expect } from '../../src/fixtures/test';
import { expectApiError, expectCustomer } from '../../src/assertions/contracts';

test('register and retrieve a customer through every search API @smoke @write', async ({ customers, customer }) => {
  const created = await expectCustomer(await customers.register(customer), 201);
  expect(created.fullName).toBe(customer.fullName);
  expect(created.customerStatus).toBe('PENDING_KYC');
  expect(created.kycStatus).toBe('PENDING');
  for (const response of [
    await customers.get(created.customerNumber),
    await customers.byEmail('  ' + customer.email.toUpperCase() + '  '),
    await customers.byPan(customer.pan.toLowerCase()),
    await customers.byPhone(customer.phone),
  ]) {
    const retrieved = await expectCustomer(response);
    expect(retrieved.customerNumber).toBe(created.customerNumber);
    expect(retrieved.fullName).toBe(customer.fullName);
  }
});

test('duplicate registration returns a business conflict @write', async ({ customers, customer }) => {
  await expectCustomer(await customers.register(customer), 201);
  await expectApiError(await customers.register(customer), 409, 'CUSTOMER_ALREADY_EXISTS');
});

for (const [field, value] of [
  ['fullName', ''], ['email', 'invalid-email'], ['phone', '123'],
  ['pan', 'invalid-pan'], ['aadhaar', '123'], ['dob', '2099-01-01'],
] as const) {
  test('registration validates ' + field + ' @write', async ({ customers, customer }) => {
    await expectApiError(await customers.register({ ...customer, [field]: value }), 400, 'INVALID_CUSTOMER');
  });
}

test('invalid customer reference returns a structured error', async ({ customers }) => {
  await expectApiError(await customers.get('INVALID'), 400, 'INVALID_CUSTOMER');
});

test('malformed JSON returns a client error @write', async ({ api }) => {
  const response = await api.post('/api/v1/customers/register', { data: '{"fullName":', headers: { 'Content-Type': 'application/json' } });
  await expectApiError(response, 400, 'INVALID_REQUEST');
});
