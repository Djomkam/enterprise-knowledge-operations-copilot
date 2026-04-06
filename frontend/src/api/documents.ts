import { apiClient } from './client'
import type { Document, ApiResponse, PagedResponse } from '../types'

export const documentsApi = {
  list: async (page = 0, size = 20): Promise<PagedResponse<Document>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<Document>>>(
      `/api/v1/documents?page=${page}&size=${size}`
    )
    return response.data.data
  },

  get: async (id: string): Promise<Document> => {
    const response = await apiClient.get<ApiResponse<Document>>(`/api/v1/documents/${id}`)
    return response.data.data
  },

  upload: async (file: File, title: string, description?: string): Promise<Document> => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', title)
    if (description) formData.append('description', description)
    const response = await apiClient.post<ApiResponse<Document>>('/api/v1/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data.data
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/documents/${id}`)
  },

  assignTeam: async (id: string, teamId: string): Promise<Document> => {
    const response = await apiClient.patch<ApiResponse<Document>>(
      `/api/v1/documents/${id}/team`,
      { teamId }
    )
    return response.data.data
  },
}
