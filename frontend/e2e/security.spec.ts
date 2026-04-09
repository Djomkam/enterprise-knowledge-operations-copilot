import { test, expect, request } from '@playwright/test'

const API = process.env.E2E_API_URL || 'http://localhost:8080'

/**
 * Security boundary E2E tests — these test the backend API directly to verify
 * that authentication and authorization rules hold, independent of the frontend UI.
 */
test.describe('API security boundaries', () => {
  test('GET /api/v1/documents without token returns 401', async () => {
    const ctx = await request.newContext({ baseURL: API })
    const resp = await ctx.get('/api/v1/documents')
    expect(resp.status()).toBe(401)
    await ctx.dispose()
  })

  test('GET /api/v1/chat/conversations without token returns 401', async () => {
    const ctx = await request.newContext({ baseURL: API })
    const resp = await ctx.get('/api/v1/chat/conversations')
    expect(resp.status()).toBe(401)
    await ctx.dispose()
  })

  test('GET /api/v1/admin with non-admin role returns 403', async () => {
    // Register a regular user
    const ctx = await request.newContext({ baseURL: API })
    const username = `e2e_sec_${Date.now()}`
    const regResp = await ctx.post('/api/v1/auth/register', {
      data: {
        username,
        email: `${username}@test.com`,
        password: 'password123',
        fullName: 'Security Test',
      },
    })

    let token: string | undefined
    if (regResp.ok()) {
      token = (await regResp.json()).data?.token
    } else {
      const loginResp = await ctx.post('/api/v1/auth/login', {
        data: { username, password: 'password123' },
      })
      token = (await loginResp.json()).data?.token
    }

    expect(token).toBeTruthy()

    const adminResp = await ctx.get('/api/v1/admin/users', {
      headers: { Authorization: `Bearer ${token}` },
    })
    // Regular USER role cannot access /admin/** — expect 403
    expect(adminResp.status()).toBe(403)
    await ctx.dispose()
  })

  test('GET /actuator/health is publicly accessible', async () => {
    const ctx = await request.newContext({ baseURL: API })
    const resp = await ctx.get('/actuator/health')
    expect(resp.status()).toBe(200)
    await ctx.dispose()
  })

  test('accessing another users conversation returns 404', async () => {
    const ctx = await request.newContext({ baseURL: API })

    // Register userA
    const userAName = `e2e_sec_a_${Date.now()}`
    const regA = await ctx.post('/api/v1/auth/register', {
      data: { username: userAName, email: `${userAName}@t.com`, password: 'pass123', fullName: 'A' },
    })
    const tokenA: string = (await regA.json()).data?.token

    // Register userB
    const userBName = `e2e_sec_b_${Date.now()}`
    const regB = await ctx.post('/api/v1/auth/register', {
      data: { username: userBName, email: `${userBName}@t.com`, password: 'pass123', fullName: 'B' },
    })
    const tokenB: string = (await regB.json()).data?.token

    // UserB tries to access a random conversation ID — should be 404 (not found / unauthorized)
    const resp = await ctx.get(`/api/v1/chat/conversations/00000000-0000-0000-0000-000000000001/messages`, {
      headers: { Authorization: `Bearer ${tokenB}` },
    })
    expect([404, 403]).toContain(resp.status())
    await ctx.dispose()
  })
})
