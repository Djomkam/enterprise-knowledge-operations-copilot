import { apiClient } from './client'
import type { ChatRequest, ChatResponse, ApiResponse, Conversation, ConversationMessage, PagedResponse } from '../types'

export const chatApi = {
  sendMessage: async (data: ChatRequest): Promise<ChatResponse> => {
    const response = await apiClient.post<ApiResponse<ChatResponse>>('/api/v1/chat', data)
    return response.data.data
  },

  listConversations: async (): Promise<Conversation[]> => {
    const response = await apiClient.get<ApiResponse<Conversation[]>>('/api/v1/chat/conversations')
    return response.data.data
  },

  getMessages: async (conversationId: string, page = 0, size = 50): Promise<PagedResponse<ConversationMessage>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<ConversationMessage>>>(
      `/api/v1/chat/conversations/${conversationId}/messages?page=${page}&size=${size}`
    )
    return response.data.data
  },

  deleteConversation: async (conversationId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/chat/conversations/${conversationId}`)
  },
}
