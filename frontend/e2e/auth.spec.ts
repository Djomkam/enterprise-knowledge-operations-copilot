import { test, expect, request } from '@playwright/test'

const API = process.env.E2E_API_URL || 'http://localhost:8080'

/**
 * Authentication flow E2E tests.
 *
 * These tests exercise the local-JWT auth path (backend running in "local" profile)
 * so they work without a running Keycloak instance.  When Keycloak is present the
 * LoginPage renders the "Sign in with Keycloak" button instead of the form — Keycloak
 * PKCE flow tests are in keycloak.spec.ts.
 */
test.describe('Login page', () => {
  test('shows login UI', async ({ page }) => {
    await page.goto('/login')
    await expect(page).toHaveURL(/login/)
    // Either the Keycloak button or the local form is visible
    const keycloakBtn = page.getByRole('button', { name: /sign in with keycloak/i })
    const localForm = page.locator('form')
    const hasKeycloak = await keycloakBtn.isVisible().catch(() => false)
    if (!hasKeycloak) {
      await expect(localForm).toBeVisible()
    }
  })

  test('unauthenticated access to /dashboard redirects to /login', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/login/)
  })

  test('local login with valid credentials navigates to dashboard', async ({ page }) => {
    // Register a test user via the API first
    const ctx = await request.newContext({ baseURL: API })
    await ctx.post('/api/v1/auth/register', {
      data: {
        username: `e2e_user_${Date.now()}`,
        email: `e2e_${Date.now()}@test.com`,
        password: 'password123',
        fullName: 'E2E Tester',
      },
    })

    const username = `e2e_login_${Date.now()}`
    await ctx.post('/api/v1/auth/register', {
      data: {
        username,
        email: `${username}@test.com`,
        password: 'password123',
        fullName: 'Login Tester',
      },
    })
    await ctx.dispose()

    await page.goto('/login')

    // Skip if Keycloak flow (no local form)
    const localForm = page.locator('form')
    if (!(await localForm.isVisible().catch(() => false))) {
      test.skip()
      return
    }

    await page.fill('input[type="text"]', username)
    await page.fill('input[type="password"]', 'password123')
    await page.click('button[type="submit"]')

    await expect(page).toHaveURL(/dashboard/, { timeout: 10_000 })
  })

  test('local login with wrong password shows error', async ({ page }) => {
    await page.goto('/login')

    const localForm = page.locator('form')
    if (!(await localForm.isVisible().catch(() => false))) {
      test.skip()
      return
    }

    await page.fill('input[type="text"]', 'nonexistent_user_xyz')
    await page.fill('input[type="password"]', 'wrongpass')
    await page.click('button[type="submit"]')

    await expect(page.locator('[style*="color: red"], [style*="color:red"]')).toBeVisible({
      timeout: 5_000,
    })
  })
})
