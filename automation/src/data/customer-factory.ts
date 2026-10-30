import { randomInt, randomUUID } from 'node:crypto';
import { CustomerRequest } from '../api/customers-client';

export function customerData(overrides: Partial<CustomerRequest> = {}): CustomerRequest {
  const letters = () => String.fromCharCode(65 + randomInt(26));
  return {
    fullName: 'Playwright Test Customer',
    email: `pw-${randomUUID()}@example.test`,
    phone: `${randomInt(6, 10)}${randomInt(1_000_000_000).toString().padStart(9, '0')}`,
    pan: `${Array.from({ length: 5 }, letters).join('')}${randomInt(10_000).toString().padStart(4, '0')}${letters()}`,
    aadhaar: String(randomInt(100_000_000_000, 1_000_000_000_000)),
    dob: '1990-06-15',
    ...overrides,
  };
}
