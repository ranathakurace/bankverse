import { APIResponse, expect } from '@playwright/test';

export async function expectCustomer(response: APIResponse, status = 200) {
  expect(response.status()).toBe(status);
  expect(response.headers()['content-type']).toContain('application/json');
  const body = await response.json();
  expect(body).toEqual(expect.objectContaining({
    customerNumber: expect.stringMatching(/^CUST\d{6}$/),
    fullName: expect.any(String), customerStatus: expect.any(String), kycStatus: expect.any(String),
  }));
  for (const field of ['pan', 'aadhaar', 'phone', 'email']) expect(body).not.toHaveProperty(field);
  return body as { customerNumber: string; fullName: string; customerStatus: string; kycStatus: string };
}

export async function expectApiError(response: APIResponse, status: number, errorCode: string) {
  expect(response.status()).toBe(status);
  expect(response.headers()['content-type']).toContain('application/json');
  expect(await response.json()).toEqual(expect.objectContaining({
    status, errorCode, message: expect.any(String), path: expect.any(String), timestamp: expect.any(String),
  }));
}
