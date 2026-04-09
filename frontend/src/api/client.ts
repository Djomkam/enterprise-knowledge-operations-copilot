import axios from 'axios'
import keycloak from '../lib/keycloak'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use(
  (config) => {
    // Prefer Keycloak token (in-memory, secure); fall back to localStorage for local dev
    const token = keycloak.authenticated && keycloak.token
      ? keycloak.token
      : localStorage.getItem('token')

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      if (keycloak.authenticated) {
        // Try refreshing the Keycloak token first
        try {
          await keycloak.updateToken(30)
          // Retry the original request with the refreshed token
          const originalRequest = error.config
          originalRequest.headers.Authorization = `Bearer ${keycloak.token}`
          return apiClient(originalRequest)
        } catch {
          keycloak.logout()
        }
      } else {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)
