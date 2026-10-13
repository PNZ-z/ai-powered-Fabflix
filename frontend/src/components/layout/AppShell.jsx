import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/authContext.js'
import LogoutButton from '../../features/auth/LogoutButton.jsx'

const customerLinks = [
  { to: '/home', label: 'Home' },
  { to: '/movies', label: 'Movies' },
  { to: '/cart', label: 'Cart' },
]

const employeeLinks = [
  { to: '/employee/login', label: 'Employee login' },
  { to: '/employee/dashboard', label: 'Dashboard' },
]

function navigationClass({ isActive }) {
  return isActive ? 'nav-link nav-link--active' : 'nav-link'
}

function AppShell() {
  const { customer } = useAuth()

  return (
    <div className="app-shell">
      <header className="site-header">
        <NavLink className="brand" to="/home" aria-label="Fabflix home">
          Fabflix
        </NavLink>

        <nav aria-label="Customer navigation">
          {customerLinks.map((link) => (
            <NavLink key={link.to} className={navigationClass} to={link.to}>
              {link.label}
            </NavLink>
          ))}
        </nav>

        <nav aria-label="Employee navigation">
          {employeeLinks.map((link) => (
            <NavLink key={link.to} className={navigationClass} to={link.to}>
              {link.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="page-content" id="main-content">
        <Outlet />
      </main>

      <footer className="site-footer">
        <span>Fabflix</span>
        {customer.status === 'authenticated'
          ? <LogoutButton />
          : <NavLink to="/login">Customer login</NavLink>}
      </footer>
    </div>
  )
}

export default AppShell
