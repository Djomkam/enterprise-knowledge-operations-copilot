import { apiClient } from './client'
import type { ChatRequest, ChatResponse, ApiResponse, Conversation, ConversationMessage, PagedResponse } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const chatApi = {
  sendMessage: async (data: ChatRequest): Promise<ChatResponse> => {
    const response = await apiClient.post<ApiResponse<ChatResponse>>('/api/v1/chat', data)
    return response.data.data
  },

  /**
   * Stream a chat response token by token via SSE.
   * onToken is called for each partial token; onDone is called with the conversationId when complete.
   * Returns a cleanup function to abort the stream.
   */
  streamMessage: (
    data: ChatRequest,
    onToken: (token: string) => void,
    onDone: (conversationId: string) => void,
    onError: (err: Error) => void,
  ): (() => void) => {
    const controller = new AbortController()
    const token = localStorage.getItem('token')

    fetch(`${API_BASE_URL}/api/v1/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
      signal: controller.signal,
    })
      .then(async (res) => {
        if (!res.ok) {
          onError(new Error(`Stream request failed: ${res.status}`))
          return
        }
        const reader = res.body!.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true })

          // SSE format: "event: token\ndata: <token>\n\n" or "event: done\ndata: <convId>\n\n"
          const lines = buffer.split('\n')
          buffer = lines.pop() ?? ''

          let eventName = ''
          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventName = line.slice(6).trim()
            } else if (line.startsWith('data:')) {
              const payload = line.slice(5).trim()
              if (eventName === 'token') onToken(payload)
              else if (eventName === 'done') onDone(payload)
              eventName = ''
            }
          }
        }
      })
      .catch((err) => {
        if (err.name !== 'AbortError') onError(err)
      })

    return () => controller.abort()
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
