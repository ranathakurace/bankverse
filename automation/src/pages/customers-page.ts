import { Page, expect } from '@playwright/test';
import { CustomerRequest } from '../api/customers-client';

export class CustomersPage {
  constructor(readonly page: Page) {}
  async goto() {
    await this.page.getByTestId('nav-customers').click();
    await expect(this.page.getByTestId('customer-registration-form')).toBeVisible();
  }
  async register(customer: CustomerRequest) {
    for (const [field, testId] of Object.entries({ fullName: 'customer-full-name', email: 'customer-email', phone: 'customer-phone', pan: 'customer-pan', aadhaar: 'customer-aadhaar', dob: 'customer-dob' })) {
      await this.page.getByTestId(testId).fill(customer[field as keyof CustomerRequest]);
    }
    const response = this.page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/customers/register' && r.request().method() === 'POST');
    await this.page.getByTestId('customer-submit').click();
    const result = await response;
    expect(result.status()).toBe(201);
    const body = await result.json();
    await expect(this.page.getByTestId('api-result-body')).toContainText(body.customerNumber);
    return body as { customerNumber: string };
  }
  async lookup(customerNumber: string) {
    const form = this.page.locator('#customer-lookup-form');
    await form.getByLabel('Customer number').fill(customerNumber);
    await form.getByRole('button', { name: 'Retrieve record' }).click();
    await expect(this.page.getByTestId('api-result-body')).toContainText(customerNumber);
  }
}
