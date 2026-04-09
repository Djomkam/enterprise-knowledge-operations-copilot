import { useState, useEffect, useRef } from 'react'
import { chatApi } from '../api/chat'
import type { Conversation, ConversationMessage } from '../types'

const sidebar: React.CSSProperties = {
  width: '240px',
  flexShrink: 0,
  backgroundColor: 'white',
  borderRadius: '8px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  display: 'flex',
  flexDirection: 'column',
  overflow: 'hidden',
}

const chatArea: React.CSSProperties = {
  flex: 1,
  backgroundColor: 'white',
  borderRadius: '8px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  display: 'flex',
  flexDirection: 'column',
  overflow: 'hidden',
  minHeight: '70vh',
}

function CitationBadge({ title, score }: { title: string; score: number }) {
  return (
    <span style={{
      display: 'inline-block',
      padding: '0.1rem 0.5rem',
      borderRadius: '4px',
      backgroundColor: '#eff6ff',
      border: '1px solid #bfdbfe',
      color: '#1d4ed8',
      fontSize: '0.75rem',
      margin: '0.15rem',
    }}>
      {title} <span style={{ opacity: 0.6 }}>({Math.round(score * 100)}%)</span>
    </span>
  )
}

const ChatPage = () => {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [messages, setMessages] = useState<ConversationMessage[]>([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [citations, setCitations] = useState<Record<string, { documentId: string; documentTitle: string; snippet: string; relevanceScore: number }[]>>({})
  const bottomRef = useRef<HTMLDivElement>(null)

  const fetchConversations = async () => {
    try {
      const list = await chatApi.listConversations()
      setConversations(list)
    } catch { /* ignore */ }
  }

  const loadMessages = async (convId: string) => {
    try {
      const paged = await chatApi.getMessages(convId)
      setMessages(paged.content.slice().reverse())
    } catch { /* ignore */ }
  }

  useEffect(() => { fetchConversations() }, [])

  useEffect(() => {
    if (activeId) loadMessages(activeId)
    else setMessages([])
  }, [activeId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault()
    if (!input.trim() || sending) return
    const text = input.trim()
    setInput('')
    setSending(true)

    // Optimistic user message
    const tempUserMsgId = `temp-user-${Date.now()}`
    const streamingMsgId = `streaming-${Date.now()}`
    setMessages(prev => [
      ...prev,
      { id: tempUserMsgId, role: 'user', content: text, createdAt: new Date().toISOString() },
      { id: streamingMsgId, role: 'assistant', content: '', createdAt: new Date().toISOString() },
    ])

    chatApi.streamMessage(
      { conversationId: activeId ?? undefined, message: text },
      (token) => {
        // Append each token to the streaming assistant message
        setMessages(prev => prev.map(m =>
          m.id === streamingMsgId ? { ...m, content: m.content + token } : m
        ))
      },
      (conversationId) => {
        // Stream complete — update with final conversationId, refresh list
        if (!activeId) {
          setActiveId(conversationId)
          fetchConversations()
        }
        setSending(false)
      },
      (_err) => {
        setMessages(prev => prev.map(m =>
          m.id === streamingMsgId
            ? { ...m, content: 'Sorry, something went wrong. Please try again.' }
            : m
        ))
        setSending(false)
      },
    )
  }

  const handleDeleteConversation = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    try {
      await chatApi.deleteConversation(id)
      setConversations(prev => prev.filter(c => c.id !== id))
      if (activeId === id) { setActiveId(null); setMessages([]) }
    } catch { /* ignore */ }
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1rem' }}>Chat</h1>
      <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start' }}>
        {/* Sidebar */}
        <div style={sidebar}>
          <div style={{ padding: '0.75rem 1rem', borderBottom: '1px solid #e5e7eb', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Conversations</span>
            <button
              onClick={() => { setActiveId(null); setMessages([]) }}
              style={{ padding: '0.25rem 0.5rem', backgroundColor: '#2563eb', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.75rem' }}
            >
              New
            </button>
          </div>
          <div style={{ flex: 1, overflowY: 'auto' }}>
            {conversations.length === 0 ? (
              <p style={{ padding: '1rem', color: '#9ca3af', fontSize: '0.85rem' }}>No conversations yet</p>
            ) : conversations.map(conv => (
              <div
                key={conv.id}
                onClick={() => setActiveId(conv.id)}
                style={{
                  padding: '0.65rem 1rem',
                  cursor: 'pointer',
                  backgroundColor: activeId === conv.id ? '#eff6ff' : 'transparent',
                  borderBottom: '1px solid #f3f4f6',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <span style={{ fontSize: '0.85rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1, color: activeId === conv.id ? '#1d4ed8' : undefined }}>
                  {conv.title || 'New conversation'}
                </span>
                <button
                  onClick={e => handleDeleteConversation(conv.id, e)}
                  style={{ marginLeft: '0.5rem', background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', fontSize: '0.85rem', flexShrink: 0 }}
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Chat area */}
        <div style={chatArea}>
          {/* Messages */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {messages.length === 0 && (
              <div style={{ textAlign: 'center', color: '#9ca3af', marginTop: '4rem' }}>
                <p style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Start a conversation</p>
                <p style={{ fontSize: '0.875rem' }}>Ask anything about your documents</p>
              </div>
            )}
            {messages.map(msg => (
              <div key={msg.id}>
                <div style={{
                  display: 'flex',
                  justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
                }}>
                  <div style={{
                    maxWidth: '75%',
                    padding: '0.75rem 1rem',
                    borderRadius: msg.role === 'user' ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                    backgroundColor: msg.role === 'user' ? '#2563eb' : '#f3f4f6',
                    color: msg.role === 'user' ? 'white' : '#111827',
                    fontSize: '0.9rem',
                    lineHeight: '1.5',
                    whiteSpace: 'pre-wrap',
                  }}>
                    {msg.content}
                  </div>
                </div>
                {citations[msg.id] && (
                  <div style={{ marginTop: '0.4rem', paddingLeft: '0.25rem' }}>
                    <span style={{ fontSize: '0.75rem', color: '#6b7280', marginRight: '0.3rem' }}>Sources:</span>
                    {citations[msg.id].map((c, i) => (
                      <CitationBadge key={i} title={c.documentTitle} score={c.relevanceScore} />
                    ))}
                  </div>
                )}
              </div>
            ))}
            {/* Streaming cursor: shown while sending and the last message is an empty assistant placeholder */}
            {sending && messages.length > 0 && messages[messages.length - 1].role === 'assistant' && messages[messages.length - 1].content === '' && (
              <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                <div style={{ padding: '0.75rem 1rem', borderRadius: '16px 16px 16px 4px', backgroundColor: '#f3f4f6', color: '#6b7280', fontSize: '0.9rem' }}>
                  ▌
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div style={{ borderTop: '1px solid #e5e7eb', padding: '0.75rem 1rem' }}>
            <form onSubmit={handleSend} style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                type="text"
                value={input}
                onChange={e => setInput(e.target.value)}
                placeholder="Ask about your documents..."
                disabled={sending}
                style={{
                  flex: 1,
                  padding: '0.6rem 0.9rem',
                  borderRadius: '8px',
                  border: '1px solid #d1d5db',
                  fontSize: '0.9rem',
                  outline: 'none',
                }}
              />
              <button
                type="submit"
                disabled={sending || !input.trim()}
                style={{
                  padding: '0.6rem 1.2rem',
                  backgroundColor: sending || !input.trim() ? '#93c5fd' : '#2563eb',
                  color: 'white',
                  border: 'none',
                  borderRadius: '8px',
                  cursor: sending || !input.trim() ? 'not-allowed' : 'pointer',
                  fontWeight: 600,
                }}
              >
                Send
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ChatPage
