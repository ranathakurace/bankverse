import { test, expect } from '../../src/fixtures/test';
import { expectCustomer } from '../../src/assertions/contracts';

test('register in UI and verify persistence through API @smoke @write', async ({ signedInPage, customers, customer, page }) => {
  await signedInPage.goto();
  const created = await signedInPage.register(customer);
  const retrieved = await expectCustomer(await customers.get(created.customerNumber));
  expect(retrieved.fullName).toBe(customer.fullName);
  await expect(page.getByTestId('api-result-body')).toContainText(customer.fullName);
});

test('API-created customer can be found in UI @write', async ({ signedInPage, customers, customer, page }) => {
  const created = await expectCustomer(await customers.register(customer), 201);
  await signedInPage.goto();
  await signedInPage.lookup(created.customerNumber);
  await expect(page.getByTestId('api-result-body')).toContainText(customer.fullName);
});
