import { apiClient } from './client'
import type { LoginRequest, LoginResponse, ApiResponse } from '../types'

export const authApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/api/v1/auth/login', data)
    return response.data.data
  },

  register: async (data: LoginRequest & { email: string; fullName?: string }): Promise<LoginResponse> => {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/api/v1/auth/register', data)
    return response.data.data
  },
}
