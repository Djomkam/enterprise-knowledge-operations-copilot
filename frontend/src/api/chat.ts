import { apiClient } from './client'
import type { ChatRequest, ChatResponse, ApiResponse } from '../types'

export const chatApi = {
  sendMessage: async (data: ChatRequest): Promise<ChatResponse> => {
    const response = await apiClient.post<ApiResponse<ChatResponse>>('/api/v1/chat', data)
    return response.data.data
  },
}
