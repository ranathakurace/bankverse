import { APIRequestContext, expect } from '@playwright/test';

export interface Session { accessToken: string; username: string; roles: string[] }

export async function login(request: APIRequestContext, username: string, password: string): Promise<Session> {
  const response = await request.post('/auth/login', { data: { username, password } });
  // Do not attach the authentication body: it contains access and refresh tokens.
  expect(response.status(), 'Employee authentication failed; check TEST_USERNAME/TEST_PASSWORD').toBe(200);
  const session: Session = await response.json();
  expect(typeof session.accessToken).toBe('string');
  expect(session.accessToken.length).toBeGreaterThan(0);
  expect(Array.isArray(session.roles)).toBe(true);
  return session;
}
