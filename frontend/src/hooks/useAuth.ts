import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import keycloak from '../lib/keycloak'
import type { LoginRequest, User } from '../types'

/**
 * Unified auth hook that supports two modes:
 *
 * 1. **Keycloak mode** (docker-compose / production) — the Keycloak instance was
 *    successfully initialized in main.tsx.  Auth state comes from the Keycloak
 *    session; login triggers a PKCE redirect to the Keycloak login page; logout
 *    ends the Keycloak session.
 *
 * 2. **Local mode** (local dev without Keycloak running) — falls back to the
 *    hand-rolled JWT stored in localStorage, identical to the previous behaviour.
 *    The test profile also takes this path.
 */
export const useAuth = () => {
  const [user, setUser] = useState<User | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const navigate = useNavigate()

  const keycloakReady = keycloak.authenticated !== undefined

  useEffect(() => {
    if (keycloakReady) {
      // Keycloak mode
      if (keycloak.authenticated && keycloak.tokenParsed) {
        const parsed = keycloak.tokenParsed as Record<string, string>
        setUser({
          username: parsed['preferred_username'] || parsed['sub'],
          email: parsed['email'] || '',
          fullName: parsed['name'] || '',
        })
        setIsAuthenticated(true)
      }
    } else {
      // Local JWT fallback
      const token = localStorage.getItem('token')
      const savedUser = localStorage.getItem('user')
      if (token && savedUser) {
        setUser(JSON.parse(savedUser))
        setIsAuthenticated(true)
      }
    }
    setIsLoading(false)
  }, [keycloakReady])

  // Token refresh — keep Keycloak session alive while the tab is open
  useEffect(() => {
    if (!keycloakReady || !keycloak.authenticated) return
    const interval = setInterval(() => {
      keycloak.updateToken(60).catch(() => keycloak.logout())
    }, 30_000)
    return () => clearInterval(interval)
  }, [keycloakReady])

  const login = async (credentials: LoginRequest) => {
    if (keycloakReady) {
      // Keycloak PKCE redirect — credentials are handled by Keycloak's login page
      keycloak.login()
      return
    }

    // Local JWT fallback
    const response = await authApi.login(credentials)
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify({
      username: response.username,
      email: response.email,
      fullName: response.fullName,
    }))
    setUser({
      username: response.username,
      email: response.email,
      fullName: response.fullName,
    })
    setIsAuthenticated(true)
    navigate('/dashboard')
  }

  const logout = () => {
    if (keycloakReady && keycloak.authenticated) {
      keycloak.logout({ redirectUri: window.location.origin + '/login' })
      return
    }
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
    setIsAuthenticated(false)
    navigate('/login')
  }

  return {
    user,
    isAuthenticated,
    isLoading,
    login,
    logout,
  }
}
