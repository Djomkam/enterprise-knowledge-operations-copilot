export interface User {
  username: string
  email: string
  fullName: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  username: string
  email: string
  fullName: string
}

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  timestamp: string
}

export interface Document {
  id: string
  title: string
  description: string
  fileName: string
  contentType: string
  fileSizeBytes: number
  status: string
  ownerId: string
  ownerUsername: string
  teamId?: string
  teamName?: string
  chunkCount?: number
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
}

export interface ChatRequest {
  conversationId?: string
  message: string
  includeContext?: boolean
}

export interface ChatResponse {
  conversationId: string
  messageId: string
  content: string
  citations?: Citation[]
  tokensUsed?: number
}

export interface Citation {
  documentId: string
  documentTitle: string
  snippet: string
  relevanceScore: number
}
