import { useState, useEffect } from 'react'
import { adminApi } from '../api/admin'
import type { AuditEvent } from '../types'

const card: React.CSSProperties = {
  backgroundColor: 'white',
  padding: '1.5rem',
  borderRadius: '8px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
}

const AUDIT_ACTIONS = ['', 'LOGIN', 'LOGOUT', 'DOCUMENT_UPLOAD', 'DOCUMENT_DELETE', 'CHAT_QUERY', 'TEAM_CREATE', 'TEAM_MEMBER_ADD', 'TEAM_MEMBER_REMOVE']

function SuccessBadge({ success }: { success: boolean }) {
  return (
    <span style={{
      display: 'inline-block',
      padding: '0.1rem 0.5rem',
      borderRadius: '999px',
      backgroundColor: success ? '#d1fae5' : '#fee2e2',
      color: success ? '#065f46' : '#b91c1c',
      fontSize: '0.75rem',
      fontWeight: 600,
    }}>
      {success ? 'OK' : 'FAIL'}
    </span>
  )
}

const AdminPage = () => {
  const [events, setEvents] = useState<AuditEvent[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [filterAction, setFilterAction] = useState('')
  const [filterUserId, setFilterUserId] = useState('')
  const [filterFrom, setFilterFrom] = useState('')
  const [filterTo, setFilterTo] = useState('')

  const fetchEvents = async (p = 0) => {
    try {
      setLoading(true)
      setError(null)
      let result
      if (filterUserId.trim()) {
        result = await adminApi.getAuditByUser(filterUserId.trim(), p)
      } else if (filterAction) {
        result = await adminApi.getAuditByAction(filterAction, p)
      } else if (filterFrom && filterTo) {
        result = await adminApi.getAuditByRange(
          new Date(filterFrom).toISOString(),
          new Date(filterTo).toISOString(),
          p
        )
      } else {
        result = await adminApi.getAuditEvents(p)
      }
      setEvents(result.content)
      setTotalPages(result.totalPages)
      setPage(p)
    } catch {
      setError('Failed to load audit events. Make sure you have ADMIN role.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchEvents(0) }, [])

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault()
    fetchEvents(0)
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>Admin — Audit Log</h1>

      {/* Filters */}
      <div style={{ ...card, marginBottom: '1.25rem' }}>
        <form onSubmit={handleFilter} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>Action</label>
            <select
              value={filterAction}
              onChange={e => setFilterAction(e.target.value)}
              style={{ padding: '0.45rem', borderRadius: '6px', border: '1px solid #d1d5db', fontSize: '0.875rem' }}
            >
              {AUDIT_ACTIONS.map(a => <option key={a} value={a}>{a || 'All actions'}</option>)}
            </select>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>User ID</label>
            <input
              type="text"
              placeholder="UUID"
              value={filterUserId}
              onChange={e => setFilterUserId(e.target.value)}
              style={{ padding: '0.45rem', borderRadius: '6px', border: '1px solid #d1d5db', fontSize: '0.875rem', width: '220px' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>From</label>
            <input
              type="datetime-local"
              value={filterFrom}
              onChange={e => setFilterFrom(e.target.value)}
              style={{ padding: '0.45rem', borderRadius: '6px', border: '1px solid #d1d5db', fontSize: '0.875rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            <label style={{ fontSize: '0.75rem', color: '#6b7280' }}>To</label>
            <input
              type="datetime-local"
              value={filterTo}
              onChange={e => setFilterTo(e.target.value)}
              style={{ padding: '0.45rem', borderRadius: '6px', border: '1px solid #d1d5db', fontSize: '0.875rem' }}
            />
          </div>
          <button
            type="submit"
            style={{ padding: '0.45rem 1rem', backgroundColor: '#2563eb', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600 }}
          >
            Filter
          </button>
          <button
            type="button"
            onClick={() => { setFilterAction(''); setFilterUserId(''); setFilterFrom(''); setFilterTo(''); setTimeout(() => fetchEvents(0), 0) }}
            style={{ padding: '0.45rem 0.75rem', backgroundColor: '#f3f4f6', color: '#374151', border: '1px solid #d1d5db', borderRadius: '6px', cursor: 'pointer' }}
          >
            Clear
          </button>
        </form>
      </div>

      {error && (
        <div style={{ ...card, backgroundColor: '#fee2e2', color: '#b91c1c', marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      <div style={card}>
        {loading ? (
          <p style={{ color: '#6b7280' }}>Loading...</p>
        ) : events.length === 0 ? (
          <p style={{ color: '#6b7280' }}>No audit events found.</p>
        ) : (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Time</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>User</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Action</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Resource</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Resource ID</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Status</th>
                  <th style={{ padding: '0.5rem 0.75rem' }}>Error</th>
                </tr>
              </thead>
              <tbody>
                {events.map(ev => (
                  <tr key={ev.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                    <td style={{ padding: '0.5rem 0.75rem', color: '#6b7280', whiteSpace: 'nowrap' }}>
                      {new Date(ev.createdAt).toLocaleString()}
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem', fontWeight: 500 }}>{ev.username}</td>
                    <td style={{ padding: '0.5rem 0.75rem' }}>
                      <span style={{ fontFamily: 'monospace', fontSize: '0.8rem', backgroundColor: '#f3f4f6', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
                        {ev.action}
                      </span>
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem', color: '#6b7280' }}>{ev.resource}</td>
                    <td style={{ padding: '0.5rem 0.75rem', color: '#6b7280', fontSize: '0.8rem', fontFamily: 'monospace' }}>
                      {ev.resourceId ? ev.resourceId.substring(0, 8) + '…' : '-'}
                    </td>
                    <td style={{ padding: '0.5rem 0.75rem' }}><SuccessBadge success={ev.success} /></td>
                    <td style={{ padding: '0.5rem 0.75rem', color: '#ef4444', fontSize: '0.8rem', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {ev.errorMessage ?? '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Pagination */}
            {totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'center', gap: '0.5rem', marginTop: '1rem' }}>
                <button
                  onClick={() => fetchEvents(page - 1)}
                  disabled={page === 0}
                  style={{ padding: '0.35rem 0.75rem', border: '1px solid #d1d5db', borderRadius: '6px', cursor: page === 0 ? 'not-allowed' : 'pointer', backgroundColor: 'white' }}
                >
                  ←
                </button>
                <span style={{ padding: '0.35rem 0.75rem', color: '#6b7280', fontSize: '0.875rem' }}>
                  Page {page + 1} / {totalPages}
                </span>
                <button
                  onClick={() => fetchEvents(page + 1)}
                  disabled={page >= totalPages - 1}
                  style={{ padding: '0.35rem 0.75rem', border: '1px solid #d1d5db', borderRadius: '6px', cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer', backgroundColor: 'white' }}
                >
                  →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

export default AdminPage
