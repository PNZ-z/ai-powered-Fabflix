import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppRoutes from './app/AppRoutes.jsx'
import { AuthContext } from './auth/authContext.js'
import { routeDefinitions } from './app/routeDefinitions.js'

const authenticatedSession = { status: 'authenticated', user: {}, error: null }
const anonymousSession = { status: 'anonymous', user: null, error: null }
const failedSession = {
  status: 'error',
  user: null,
  error: new Error('Unable to reach the Fabflix server.'),
}

function renderRoute(path, sessions = {}) {
  const auth = {
    customer: authenticatedSession,
    employee: authenticatedSession,
    refreshCustomer: () => {},
    refreshEmployee: () => {},
    ...sessions,
  }

  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider value={auth}>
        <AppRoutes />
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

describe('application routing', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify([{ name: 'Drama' }]),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it.each(routeDefinitions)('renders the $title placeholder at $samplePath', (route) => {
    renderRoute(route.samplePath)

    expect(screen.getByRole('heading', { name: route.title })).toBeInTheDocument()
  })

  it('uses client-side navigation from the shared shell', async () => {
    const user = userEvent.setup()
    renderRoute('/home')

    await user.click(screen.getByRole('link', { name: 'Movies' }))

    expect(screen.getByRole('heading', { name: 'Movie catalog' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Movies' })).toHaveAttribute('aria-current', 'page')
  })

  it('redirects the root route to the customer home page', async () => {
    renderRoute('/')

    expect(await screen.findByRole('heading', { name: 'Find your next Fabflix movie' })).toBeInTheDocument()
  })

  it('renders the requested identifier on detail placeholders', () => {
    renderRoute('/movies/tt7654321')

    expect(screen.getByText('Requested ID: tt7654321')).toBeInTheDocument()
  })

  it('redirects anonymous customers to customer login', async () => {
    renderRoute('/cart', { customer: anonymousSession })

    expect(await screen.findByRole('heading', { name: 'Sign in to Fabflix' })).toBeInTheDocument()
  })

  it('redirects anonymous employees to employee login', async () => {
    renderRoute('/employee/dashboard', { employee: anonymousSession })

    expect(await screen.findByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
  })

  it('shows a user-facing error when session validation fails', () => {
    renderRoute('/movies', { customer: failedSession })

    expect(screen.getByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('renders a recovery link for unknown routes', async () => {
    const user = userEvent.setup()
    renderRoute('/missing-page')

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByText('No Fabflix page matches /missing-page.')).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Return home' }))

    expect(screen.getByRole('heading', { name: 'Find your next Fabflix movie' })).toBeInTheDocument()
  })
})
