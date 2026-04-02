import { NavLink } from 'react-router-dom'

const Sidebar = () => {
  const navItems = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/documents', label: 'Documents' },
    { path: '/chat', label: 'Chat' },
    { path: '/admin', label: 'Admin' },
  ]

  return (
    <aside style={{
      width: '250px',
      backgroundColor: '#2c3e50',
      color: 'white',
      padding: '2rem 0',
    }}>
      <nav>
        <ul style={{ listStyle: 'none' }}>
          {navItems.map((item) => (
            <li key={item.path} style={{ margin: '0.5rem 0' }}>
              <NavLink
                to={item.path}
                style={({ isActive }) => ({
                  display: 'block',
                  padding: '0.75rem 2rem',
                  color: 'white',
                  textDecoration: 'none',
                  backgroundColor: isActive ? '#34495e' : 'transparent',
                  borderLeft: isActive ? '4px solid #3498db' : '4px solid transparent',
                })}
              >
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  )
}

export default Sidebar
