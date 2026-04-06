import { useState, useEffect, useRef } from 'react'
import { documentsApi } from '../api/documents'
import type { Document } from '../types'

const card: React.CSSProperties = {
  backgroundColor: 'white',
  padding: '1.5rem',
  borderRadius: '8px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
}

const btn = (color = '#2563eb'): React.CSSProperties => ({
  padding: '0.4rem 0.9rem',
  backgroundColor: color,
  color: 'white',
  border: 'none',
  borderRadius: '6px',
  cursor: 'pointer',
  fontSize: '0.85rem',
})

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    PENDING: '#f59e0b',
    PROCESSING: '#3b82f6',
    READY: '#10b981',
    FAILED: '#ef4444',
  }
  return (
    <span style={{
      display: 'inline-block',
      padding: '0.15rem 0.6rem',
      borderRadius: '999px',
      backgroundColor: colors[status] ?? '#6b7280',
      color: 'white',
      fontSize: '0.75rem',
      fontWeight: 600,
    }}>
      {status}
    </span>
  )
}

const DocumentsPage = () => {
  const [documents, setDocuments] = useState<Document[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadTitle, setUploadTitle] = useState('')
  const [uploadDescription, setUploadDescription] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [showUpload, setShowUpload] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const fetchDocuments = async () => {
    try {
      setLoading(true)
      const paged = await documentsApi.list()
      setDocuments(paged.content)
    } catch {
      setError('Failed to load documents')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchDocuments() }, [])

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedFile || !uploadTitle.trim()) return
    try {
      setUploading(true)
      await documentsApi.upload(selectedFile, uploadTitle.trim(), uploadDescription.trim() || undefined)
      setShowUpload(false)
      setUploadTitle('')
      setUploadDescription('')
      setSelectedFile(null)
      if (fileRef.current) fileRef.current.value = ''
      fetchDocuments()
    } catch {
      setError('Upload failed')
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await documentsApi.delete(id)
      setDeleteConfirm(null)
      setDocuments(prev => prev.filter(d => d.id !== id))
    } catch {
      setError('Delete failed')
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <h1>Documents</h1>
        <button style={btn()} onClick={() => setShowUpload(v => !v)}>
          {showUpload ? 'Cancel' : '+ Upload'}
        </button>
      </div>

      {error && (
        <div style={{ ...card, backgroundColor: '#fee2e2', color: '#b91c1c', marginBottom: '1rem' }}>
          {error}
          <button style={{ marginLeft: '1rem', background: 'none', border: 'none', cursor: 'pointer', color: '#b91c1c' }} onClick={() => setError(null)}>✕</button>
        </div>
      )}

      {showUpload && (
        <div style={{ ...card, marginBottom: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem' }}>Upload Document</h3>
          <form onSubmit={handleUpload}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <input
                type="text"
                placeholder="Title *"
                value={uploadTitle}
                onChange={e => setUploadTitle(e.target.value)}
                required
                style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db' }}
              />
              <input
                type="text"
                placeholder="Description (optional)"
                value={uploadDescription}
                onChange={e => setUploadDescription(e.target.value)}
                style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #d1d5db' }}
              />
              <input
                ref={fileRef}
                type="file"
                accept=".pdf,.txt,.md"
                onChange={e => setSelectedFile(e.target.files?.[0] ?? null)}
                required
                style={{ padding: '0.4rem' }}
              />
              <div>
                <button type="submit" style={btn()} disabled={uploading || !selectedFile || !uploadTitle.trim()}>
                  {uploading ? 'Uploading...' : 'Upload'}
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      <div style={card}>
        {loading ? (
          <p style={{ color: '#6b7280' }}>Loading...</p>
        ) : documents.length === 0 ? (
          <p style={{ color: '#6b7280' }}>No documents yet. Upload your first document above.</p>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #e5e7eb', textAlign: 'left' }}>
                <th style={{ padding: '0.5rem 0.75rem' }}>Title</th>
                <th style={{ padding: '0.5rem 0.75rem' }}>File</th>
                <th style={{ padding: '0.5rem 0.75rem' }}>Size</th>
                <th style={{ padding: '0.5rem 0.75rem' }}>Status</th>
                <th style={{ padding: '0.5rem 0.75rem' }}>Owner</th>
                <th style={{ padding: '0.5rem 0.75rem' }}>Uploaded</th>
                <th style={{ padding: '0.5rem 0.75rem' }}></th>
              </tr>
            </thead>
            <tbody>
              {documents.map(doc => (
                <tr key={doc.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                  <td style={{ padding: '0.6rem 0.75rem', fontWeight: 500 }}>{doc.title}</td>
                  <td style={{ padding: '0.6rem 0.75rem', color: '#6b7280', fontSize: '0.875rem' }}>{doc.fileName}</td>
                  <td style={{ padding: '0.6rem 0.75rem', color: '#6b7280', fontSize: '0.875rem' }}>{formatBytes(doc.fileSizeBytes)}</td>
                  <td style={{ padding: '0.6rem 0.75rem' }}><StatusBadge status={doc.status} /></td>
                  <td style={{ padding: '0.6rem 0.75rem', color: '#6b7280', fontSize: '0.875rem' }}>{doc.ownerUsername}</td>
                  <td style={{ padding: '0.6rem 0.75rem', color: '#6b7280', fontSize: '0.875rem' }}>
                    {new Date(doc.createdAt).toLocaleDateString()}
                  </td>
                  <td style={{ padding: '0.6rem 0.75rem' }}>
                    {deleteConfirm === doc.id ? (
                      <span>
                        <span style={{ fontSize: '0.8rem', marginRight: '0.5rem' }}>Confirm?</span>
                        <button style={btn('#ef4444')} onClick={() => handleDelete(doc.id)}>Yes</button>
                        <button style={{ ...btn('#6b7280'), marginLeft: '0.3rem' }} onClick={() => setDeleteConfirm(null)}>No</button>
                      </span>
                    ) : (
                      <button style={btn('#ef4444')} onClick={() => setDeleteConfirm(doc.id)}>Delete</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default DocumentsPage
