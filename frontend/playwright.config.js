import { defineConfig, devices } from '@playwright/test'

// Browser-level smoke tests for the migrated React app. Every API call is
// mocked with page.route (see e2e/support/api.js), so no backend or MySQL is
// needed. Only chromium runs, at the two widths the issue calls out: 360
// (phone) and 1440 (desktop).
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium-360',
      use: { ...devices['Desktop Chrome'], viewport: { width: 360, height: 800 } },
    },
    {
      name: 'chromium-1440',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
  ],
  webServer: {
    // Bind explicitly to 127.0.0.1: `vite preview` otherwise listens on
    // `localhost`, which resolves to IPv6 (::1) on some machines and would
    // never answer the baseURL below.
    command: 'npm run build && npm run preview -- --host 127.0.0.1 --port 4173 --strictPort',
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
