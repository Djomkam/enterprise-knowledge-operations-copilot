import { apiClient } from './client'
import type { ApiResponse, PagedResponse, AuditEvent } from '../types'

export const adminApi = {
  getAuditEvents: async (page = 0, size = 50): Promise<PagedResponse<AuditEvent>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<AuditEvent>>>(
      `/api/v1/admin/audit?page=${page}&size=${size}`
    )
    return response.data.data
  },

  getAuditByUser: async (userId: string, page = 0, size = 50): Promise<PagedResponse<AuditEvent>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<AuditEvent>>>(
      `/api/v1/admin/audit/user/${userId}?page=${page}&size=${size}`
    )
    return response.data.data
  },

  getAuditByAction: async (action: string, page = 0, size = 50): Promise<PagedResponse<AuditEvent>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<AuditEvent>>>(
      `/api/v1/admin/audit/action/${action}?page=${page}&size=${size}`
    )
    return response.data.data
  },

  getAuditByRange: async (
    from: string,
    to: string,
    page = 0,
    size = 50
  ): Promise<PagedResponse<AuditEvent>> => {
    const response = await apiClient.get<ApiResponse<PagedResponse<AuditEvent>>>(
      `/api/v1/admin/audit/range?from=${from}&to=${to}&page=${page}&size=${size}`
    )
    return response.data.data
  },
}
