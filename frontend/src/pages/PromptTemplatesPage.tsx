import { useState, useEffect } from 'react'
import { apiClient } from '../api/client'
import type { ApiResponse, PagedResponse } from '../types'

interface PromptTemplate {
  id: string
  name: string
  description?: string
  systemPrompt: string
  roleType?: string
  teamId?: string
  teamName?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

interface TemplateForm {
  name: string
  description: string
  systemPrompt: string
  roleType: string
}

const card: React.CSSProperties = {
  backgroundColor: 'white',
  padding: '1.5rem',
  borderRadius: '8px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  marginBottom: '1rem',
}

const templateApi = {
  list: (page = 0) =>
    apiClient.get<ApiResponse<PagedResponse<PromptTemplate>>>(
      `/api/v1/admin/prompt-templates?page=${page}&size=20`),
  create: (body: TemplateForm) =>
    apiClient.post<ApiResponse<PromptTemplate>>('/api/v1/admin/prompt-templates', body),
  update: (id: string, body: TemplateForm) =>
    apiClient.put<ApiResponse<PromptTemplate>>(`/api/v1/admin/prompt-templates/${id}`, body),
  deactivate: (id: string) =>
    apiClient.delete(`/api/v1/admin/prompt-templates/${id}`),
}

const ROLES = ['', 'USER', 'ADMIN', 'ANALYST']

const emptyForm: TemplateForm = { name: '', description: '', systemPrompt: '', roleType: '' }

const PromptTemplatesPage = () => {
  const [templates, setTemplates] = useState<PromptTemplate[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<TemplateForm>(emptyForm)
  const [saving, setSaving] = useState(false)

  const fetch = async () => {
    try {
      setLoading(true)
      const res = await templateApi.list()
      setTemplates(res.data.data.content)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetch() }, [])

  const openCreate = () => { setForm(emptyForm); setEditingId(null); setShowForm(true) }
  const openEdit = (t: PromptTemplate) => {
    setForm({ name: t.name, description: t.description ?? '', systemPrompt: t.systemPrompt, roleType: t.roleType ?? '' })
    setEditingId(t.id)
    setShowForm(true)
  }

  const handleSave = async () => {
    if (!form.name.trim() || !form.systemPrompt.trim()) return
    try {
      setSaving(true)
      setError(null)
      if (editingId) {
        await templateApi.update(editingId, form)
      } else {
        await templateApi.create(form)
      }
      setShowForm(false)
      fetch()
    } catch (e: any) {
      setError(e.response?.data?.message ?? e.message)
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (id: string) => {
    if (!confirm('Deactivate this template?')) return
    try {
      await templateApi.deactivate(id)
      fetch()
    } catch (e: any) {
      setError(e.response?.data?.message ?? e.message)
    }
  }

  return (
    <div style={{ padding: '2rem', maxWidth: '1000px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h1 style={{ margin: 0 }}>Prompt Templates</h1>
        <button onClick={openCreate} style={{
          padding: '0.5rem 1.25rem', backgroundColor: '#2563eb', color: 'white',
          border: 'none', borderRadius: '6px', cursor: 'pointer',
        }}>
          New Template
        </button>
      </div>

      {error && (
        <div style={{ backgroundColor: '#fee2e2', color: '#b91c1c', padding: '1rem', borderRadius: '6px', marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {/* Create/Edit form */}
      {showForm && (
        <div style={{ ...card, border: '2px solid #2563eb', marginBottom: '2rem' }}>
          <h2 style={{ marginTop: 0 }}>{editingId ? 'Edit Template' : 'New Template'}</h2>
          <div style={{ display: 'grid', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>Name *</label>
              <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: '4px', boxSizing: 'border-box' }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>Description</label>
              <input value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: '4px', boxSizing: 'border-box' }} />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>Applies to Role</label>
              <select value={form.roleType} onChange={e => setForm(f => ({ ...f, roleType: e.target.value }))}
                style={{ padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: '4px', minWidth: '150px' }}>
                {ROLES.map(r => <option key={r} value={r}>{r || '(all roles)'}</option>)}
              </select>
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '0.25rem', fontWeight: 600 }}>System Prompt *</label>
              <textarea value={form.systemPrompt} onChange={e => setForm(f => ({ ...f, systemPrompt: e.target.value }))}
                rows={8}
                style={{ width: '100%', padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: '4px', boxSizing: 'border-box', fontFamily: 'monospace', fontSize: '0.875rem' }} />
            </div>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <button onClick={handleSave} disabled={saving} style={{
                padding: '0.5rem 1.25rem', backgroundColor: saving ? '#93c5fd' : '#2563eb', color: 'white',
                border: 'none', borderRadius: '6px', cursor: saving ? 'default' : 'pointer',
              }}>
                {saving ? 'Saving…' : 'Save'}
              </button>
              <button onClick={() => setShowForm(false)} style={{
                padding: '0.5rem 1.25rem', backgroundColor: 'white', border: '1px solid #d1d5db',
                borderRadius: '6px', cursor: 'pointer',
              }}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Template list */}
      {loading ? (
        <p style={{ color: '#6b7280' }}>Loading…</p>
      ) : templates.length === 0 ? (
        <div style={card}><p style={{ color: '#9ca3af', textAlign: 'center' }}>No templates yet.</p></div>
      ) : (
        templates.map(t => (
          <div key={t.id} style={card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <h3 style={{ margin: '0 0 0.25rem 0' }}>{t.name}</h3>
                {t.description && <p style={{ margin: '0 0 0.5rem 0', color: '#6b7280', fontSize: '0.875rem' }}>{t.description}</p>}
                {t.roleType && (
                  <span style={{ display: 'inline-block', padding: '0.1rem 0.5rem', backgroundColor: '#dbeafe', color: '#1e40af', borderRadius: '999px', fontSize: '0.75rem', marginBottom: '0.5rem' }}>
                    Role: {t.roleType}
                  </span>
                )}
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', flexShrink: 0, marginLeft: '1rem' }}>
                <button onClick={() => openEdit(t)} style={{ padding: '0.3rem 0.75rem', cursor: 'pointer', borderRadius: '4px', border: '1px solid #d1d5db' }}>Edit</button>
                {t.name !== 'Default' && (
                  <button onClick={() => handleDeactivate(t.id)} style={{ padding: '0.3rem 0.75rem', cursor: 'pointer', borderRadius: '4px', border: '1px solid #fca5a5', color: '#b91c1c', backgroundColor: 'white' }}>
                    Deactivate
                  </button>
                )}
              </div>
            </div>
            <pre style={{
              backgroundColor: '#f9fafb', padding: '0.75rem', borderRadius: '4px',
              fontSize: '0.8rem', overflow: 'auto', maxHeight: '150px', marginTop: '0.75rem',
              whiteSpace: 'pre-wrap', border: '1px solid #e5e7eb',
            }}>{t.systemPrompt}</pre>
          </div>
        ))
      )}
    </div>
  )
}

export default PromptTemplatesPage
