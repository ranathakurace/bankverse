import { APIRequestContext } from '@playwright/test';

export interface CustomerRequest {
  fullName: string; email: string; phone: string; pan: string; aadhaar: string; dob: string;
}

export class CustomersClient {
  constructor(private readonly request: APIRequestContext) {}
  register(data: CustomerRequest) { return this.request.post('/api/v1/customers/register', { data }); }
  get(customerNumber: string) { return this.request.get(`/api/v1/customers/${encodeURIComponent(customerNumber)}`); }
  byEmail(email: string) { return this.request.get('/api/v1/customers/search/email', { params: { email } }); }
  byPan(pan: string) { return this.request.get('/api/v1/customers/search', { params: { pan } }); }
  byPhone(phone: string) { return this.request.get('/api/v1/customers/search/phone', { params: { phone } }); }
}
