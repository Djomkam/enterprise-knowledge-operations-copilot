import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright E2E configuration.
 *
 * Tests assume the full stack is running:
 *   - Frontend:  http://localhost:5173  (vite dev server)
 *   - Backend:   http://localhost:8080
 *   - Keycloak:  http://localhost:8180  (optional — tests detect presence)
 *
 * To run:
 *   cd frontend && npm run test:e2e
 *
 * Environment variables:
 *   E2E_BASE_URL   — override frontend base URL (default: http://localhost:5173)
 *   E2E_API_URL    — override backend base URL  (default: http://localhost:8080)
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false, // run sequentially to avoid DB conflicts
  retries: process.env.CI ? 2 : 0,
  reporter: [['html', { outputFolder: 'playwright-report' }], ['list']],
  timeout: 30_000,

  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
