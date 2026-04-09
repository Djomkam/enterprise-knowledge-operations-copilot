import { useState, useEffect } from 'react'
import { apiClient } from '../api/client'
import type { ApiResponse, PagedResponse } from '../types'

interface QueryAnalytic {
  id: string
  userId: string
  teamId?: string
  conversationId?: string
  query: string
  responseLength?: number
  retrievalHits?: number
  latencyMs?: number
  tokensUsed?: number
  modelUsed?: string
  success: boolean
  errorMessage?: string
  createdAt: string
}

interface AnalyticsSummary {
  totalQueries: number
  successfulQueries: number
  avgLatencyMs?: number
  successRate: number
}

const card: React.CSSProperties = {
  backgroundColor: 'white',
  padding: '1.5rem',
  borderRadius: '8px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
}

const analyticsApi = {
  getAll: (page = 0, size = 50) =>
    apiClient.get<ApiResponse<PagedResponse<QueryAnalytic>>>(
      `/api/v1/admin/analytics?page=${page}&size=${size}`),
  getSummary: (from?: string, to?: string) => {
    const params = new URLSearchParams()
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    return apiClient.get<ApiResponse<AnalyticsSummary>>(
      `/api/v1/admin/analytics/summary?${params}`)
  },
}

const AnalyticsPage = () => {
  const [events, setEvents] = useState<QueryAnalytic[]>([])
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const fetchData = async (p = 0) => {
    try {
      setLoading(true)
      setError(null)
      const [eventsRes, summaryRes] = await Promise.all([
        analyticsApi.getAll(p),
        analyticsApi.getSummary(),
      ])
      setEvents(eventsRes.data.data.content)
      setTotalPages(eventsRes.data.data.totalPages)
      setSummary(summaryRes.data.data)
    } catch (e: any) {
      setError(e.message ?? 'Failed to load analytics')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData(page) }, [page])

  const fmt = (ms?: number) => ms != null ? `${ms.toFixed(0)} ms` : '—'
  const fmtDate = (s: string) => new Date(s).toLocaleString()

  return (
    <div style={{ padding: '2rem', maxWidth: '1400px', margin: '0 auto' }}>
      <h1 style={{ marginBottom: '1.5rem' }}>Query Analytics</h1>

      {error && (
        <div style={{ backgroundColor: '#fee2e2', color: '#b91c1c', padding: '1rem', borderRadius: '6px', marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {/* Summary cards */}
      {summary && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', marginBottom: '2rem' }}>
          {[
            { label: 'Total Queries (24 h)', value: summary.totalQueries },
            { label: 'Successful', value: summary.successfulQueries },
            { label: 'Success Rate', value: `${summary.successRate.toFixed(1)}%` },
            { label: 'Avg Latency', value: fmt(summary.avgLatencyMs ?? undefined) },
          ].map(({ label, value }) => (
            <div key={label} style={card}>
              <div style={{ fontSize: '0.85rem', color: '#6b7280', marginBottom: '0.5rem' }}>{label}</div>
              <div style={{ fontSize: '1.75rem', fontWeight: 700 }}>{value}</div>
            </div>
          ))}
        </div>
      )}

      {/* Events table */}
      <div style={card}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <h2 style={{ margin: 0 }}>Recent Queries</h2>
          <button
            onClick={() => fetchData(page)}
            style={{ padding: '0.4rem 1rem', cursor: 'pointer', borderRadius: '4px', border: '1px solid #d1d5db' }}
          >
            Refresh
          </button>
        </div>

        {loading ? (
          <p style={{ color: '#6b7280' }}>Loading…</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e5e7eb' }}>
                  {['Time', 'Query', 'Hits', 'Latency', 'Tokens', 'Status'].map(h => (
                    <th key={h} style={{ textAlign: 'left', padding: '0.5rem', whiteSpace: 'nowrap' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {events.map(e => (
                  <tr key={e.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '0.5rem', whiteSpace: 'nowrap', color: '#6b7280' }}>{fmtDate(e.createdAt)}</td>
                    <td style={{ padding: '0.5rem', maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                        title={e.query}>{e.query}</td>
                    <td style={{ padding: '0.5rem' }}>{e.retrievalHits ?? 0}</td>
                    <td style={{ padding: '0.5rem' }}>{fmt(e.latencyMs ?? undefined)}</td>
                    <td style={{ padding: '0.5rem' }}>{e.tokensUsed ?? '—'}</td>
                    <td style={{ padding: '0.5rem' }}>
                      <span style={{
                        display: 'inline-block',
                        padding: '0.1rem 0.5rem',
                        borderRadius: '999px',
                        backgroundColor: e.success ? '#d1fae5' : '#fee2e2',
                        color: e.success ? '#065f46' : '#b91c1c',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                      }}>
                        {e.success ? 'OK' : 'FAIL'}
                      </span>
                    </td>
                  </tr>
                ))}
                {events.length === 0 && (
                  <tr>
                    <td colSpan={6} style={{ padding: '1rem', textAlign: 'center', color: '#9ca3af' }}>
                      No analytics data yet. Queries will appear here after chat interactions.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '0.5rem', marginTop: '1rem' }}>
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              style={{ padding: '0.4rem 0.8rem', cursor: 'pointer', borderRadius: '4px', border: '1px solid #d1d5db' }}>
              Prev
            </button>
            <span style={{ padding: '0.4rem 0.8rem' }}>Page {page + 1} / {totalPages}</span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page === totalPages - 1}
              style={{ padding: '0.4rem 0.8rem', cursor: 'pointer', borderRadius: '4px', border: '1px solid #d1d5db' }}>
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default AnalyticsPage
