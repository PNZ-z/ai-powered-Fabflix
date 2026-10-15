import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/authContext.js'
import LogoutButton from '../../features/auth/LogoutButton.jsx'
import CartBadge from '../../features/cart/CartBadge.jsx'
import CartStatusProvider from '../../features/cart/CartStatusProvider.jsx'
import { useCartStatus } from '../../features/cart/cartStatusContext.js'

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

// The badge only fits a digit or "...", so the Cart link's accessible name
// always carries the real piece count.
function cartLinkName(count) {
  if (count <= 0) return 'Cart'
  return count === 1 ? 'Cart, 1 item in your cart' : `Cart, ${count} items in your cart`
}

function AppShellLayout() {
  const { customer } = useAuth()
  const { cartCount } = useCartStatus()

  return (
    <div className="app-shell">
      <header className="site-header">
        <NavLink className="brand" to="/home" aria-label="Fabflix home">
          Fabflix
        </NavLink>

        <nav aria-label="Customer navigation">
          {customerLinks.map((link) => (
            <NavLink
              key={link.to}
              className={navigationClass}
              to={link.to}
              aria-label={link.to === '/cart' ? cartLinkName(cartCount) : undefined}
            >
              {link.label}
              {link.to === '/cart' && <CartBadge count={cartCount} />}
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

// The cart status provider sits above the header and the routed pages, so the
// nav badge and every add-to-cart control share one cart state.
function AppShell() {
  return (
    <CartStatusProvider>
      <AppShellLayout />
    </CartStatusProvider>
  )
}

export default AppShell
