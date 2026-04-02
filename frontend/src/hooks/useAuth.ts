import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import type { LoginRequest, User } from '../types'

export const useAuth = () => {
  const [user, setUser] = useState<User | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const token = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')

    if (token && savedUser) {
      setUser(JSON.parse(savedUser))
      setIsAuthenticated(true)
    }
    setIsLoading(false)
  }, [])

  const login = async (credentials: LoginRequest) => {
    try {
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
    } catch (error) {
      throw error
    }
  }

  const logout = () => {
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
