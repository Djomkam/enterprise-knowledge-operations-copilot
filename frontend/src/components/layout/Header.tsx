import { useAuth } from '../../hooks/useAuth'

const Header = () => {
  const { user, logout } = useAuth()

  return (
    <header style={{
      padding: '1rem 2rem',
      backgroundColor: 'white',
      borderBottom: '1px solid #e0e0e0',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
    }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 600 }}>Enterprise Knowledge Copilot</h1>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
        <span>{user?.fullName || user?.username}</span>
        <button onClick={logout} style={{
          padding: '0.5rem 1rem',
          backgroundColor: '#dc3545',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
        }}>
          Logout
        </button>
      </div>
    </header>
  )
}

export default Header
