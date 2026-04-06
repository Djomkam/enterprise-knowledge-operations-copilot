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

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface Conversation {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface ConversationMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  createdAt: string
}

export interface AuditEvent {
  id: string
  userId?: string
  username: string
  action: string
  resource: string
  resourceId?: string
  details?: string
  ipAddress?: string
  success: boolean
  errorMessage?: string
  createdAt: string
}
