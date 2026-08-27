import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  // Run ONLY the tests inside the e2e folder
  testDir: './e2e',
  testIgnore: ['**/src/**'],

  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
  },

  // Optional: automatically start `npm start` before tests if the server isn't already running
  webServer: {
    command: 'npm start',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});