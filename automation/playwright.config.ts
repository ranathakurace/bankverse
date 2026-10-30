import { defineConfig, devices } from '@playwright/test';
import { environment as env } from './config/environment';

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  forbidOnly: env.ci,
  failOnFlakyTests: env.ci,
  retries: env.ci ? 1 : 0,
  workers: env.workers,
  grepInvert: env.allowTestData ? undefined : /@write/,
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
  ],
  use: {
    baseURL: env.uiBaseUrl,
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
    ignoreHTTPSErrors: false,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
  },
  projects: [
    { name: 'setup', testMatch: /environment\.setup\.ts/, use: { trace: 'off' } },
    { name: 'api', testDir: './tests/api', testMatch: '**/*.spec.ts', dependencies: ['setup'], use: { trace: 'off' } },
    ...(['chromium', 'firefox', 'webkit'] as const).map(name => ({
      name, testDir: './tests/ui', testMatch: '**/*.spec.ts', dependencies: ['setup'],
      use: { ...(name === 'chromium' ? { channel: 'chromium' } : {}), ...devices[name === 'chromium' ? 'Desktop Chrome' : name === 'firefox' ? 'Desktop Firefox' : 'Desktop Safari'] },
    })),
  ],
  webServer: env.startApp ? {
    command: 'node scripts/start-app.cjs',
    url: `${env.apiBaseUrl}/health`,
    timeout: 180_000,
    reuseExistingServer: false,
    stdout: 'pipe',
    stderr: 'pipe',
    env: { TEST_PORT: String(env.port) },
  } : undefined,
});
