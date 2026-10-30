import { config } from 'dotenv';
import path from 'node:path';

config({ path: path.resolve(__dirname, '../.env'), quiet: true });

function boolean(name: string, fallback: boolean): boolean {
  const value = process.env[name];
  if (value === undefined) return fallback;
  if (value !== 'true' && value !== 'false') throw new Error(`${name} must be true or false`);
  return value === 'true';
}

function integer(name: string, fallback: number, max: number): number {
  const value = Number(process.env[name] ?? fallback);
  if (!Number.isInteger(value) || value < 1 || value > max) throw new Error(`${name} is out of range`);
  return value;
}

function baseUrl(name: string, fallback: string): string {
  const url = new URL(process.env[name] ?? fallback);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.pathname !== '/' || url.search || url.hash) {
    throw new Error(`${name} must be an HTTP(S) origin without credentials, query, or path`);
  }
  return url.origin;
}

const startApp = boolean('START_APP', true);
const port = integer('TEST_PORT', 18080, 65535);
const localUrl = `http://127.0.0.1:${port}`;
const apiBaseUrl = baseUrl('API_BASE_URL', localUrl);
const uiBaseUrl = baseUrl('UI_BASE_URL', localUrl);
if (startApp && (apiBaseUrl !== localUrl || uiBaseUrl !== localUrl)) {
  throw new Error('Managed mode requires local URLs matching TEST_PORT. Set START_APP=false for an existing environment.');
}

export const environment = {
  startApp, port, apiBaseUrl, uiBaseUrl,
  ci: boolean('CI', false),
  workers: integer('TEST_WORKERS', 2, 32),
  allowTestData: startApp || boolean('ALLOW_TEST_DATA', false),
  username: process.env.TEST_USERNAME ?? (startApp ? 'playwright-admin' : ''),
  password: process.env.TEST_PASSWORD ?? (startApp ? 'PlaywrightTest2026!' : ''),
};
