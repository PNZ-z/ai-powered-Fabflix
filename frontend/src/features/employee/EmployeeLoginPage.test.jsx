import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import { expectNoA11yViolations } from '../../test/axe.js'
import EmployeeLoginPage from './EmployeeLoginPage.jsx'

const anonymousEmployee = { status: 'anonymous', user: null, error: null }

function LocationProbe() {
  const location = useLocation()
  return <p>Location: {location.pathname}{location.search}{location.hash}</p>
}

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

function renderLogin(refreshEmployee = vi.fn().mockResolvedValue(undefined)) {
  render(
    <MemoryRouter initialEntries={['/employee/login']}>
      <AuthContext.Provider value={{ employee: anonymousEmployee, refreshEmployee }}>
        <Routes>
          <Route path="employee/login" element={<EmployeeLoginPage />} />
          <Route path="employee/dashboard" element={<h1>Employee dashboard</h1>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
  return refreshEmployee
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('EmployeeLoginPage', () => {
  it('requires both fields and renders no reCAPTCHA widget', () => {
    vi.stubGlobal('fetch', vi.fn())
    renderLogin()

    expect(screen.getByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toBeRequired()
    expect(screen.getByLabelText('Password')).toBeRequired()
    expect(screen.queryByText(/verification/i)).not.toBeInTheDocument()
  })

  it('logs in, refreshes the employee session, and navigates to the dashboard', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ status: 'success' }))
    vi.stubGlobal('fetch', fetchMock)
    const refreshEmployee = renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Username'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Employee dashboard' })).toBeInTheDocument()
    expect(refreshEmployee).toHaveBeenCalledOnce()
    const request = fetchMock.mock.calls[0]
    expect(request[0]).toBe('/api/boot/employee-login')
    expect(request[1].method).toBe('POST')
    expect(request[1].body.get('username')).toBe('admin@example.com')
    expect(request[1].body.get('password')).toBe('secret')
  })

  it('shows the server failure message without navigating', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({
      status: 'fail',
      message: 'Invalid employee email or password.',
    })))
    renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Username'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert'))
      .toHaveTextContent('Invalid employee email or password.')
    expect(screen.getByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
  })

  it('shows a safe message when the server is unreachable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')))
    renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Username'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
  })

  it('disables the submit button while the request is in flight', async () => {
    let resolveFetch
    vi.stubGlobal('fetch', vi.fn(() => new Promise((resolve) => {
      resolveFetch = resolve
    })))
    renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Username'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(screen.getByRole('button', { name: 'Signing in...' })).toBeDisabled()

    resolveFetch(jsonResponse({ status: 'success' }))
    expect(await screen.findByRole('heading', { name: 'Employee dashboard' })).toBeInTheDocument()
  })

  it('has no detectable accessibility violations', async () => {
    vi.stubGlobal('fetch', vi.fn())
    renderLogin()

    await screen.findByRole('heading', { name: 'Employee login' })
    await expectNoA11yViolations()
  })

  it('redirects an authenticated employee to the dashboard', () => {
    vi.stubGlobal('fetch', vi.fn())
    render(
      <MemoryRouter initialEntries={['/employee/login']}>
        <AuthContext.Provider value={{
          employee: { status: 'authenticated', user: {}, error: null },
          refreshEmployee: vi.fn(),
        }}
        >
          <Routes>
            <Route path="employee/login" element={<EmployeeLoginPage />} />
            <Route path="employee/dashboard" element={<h1>Employee dashboard</h1>} />
          </Routes>
        </AuthContext.Provider>
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: 'Employee dashboard' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Employee login' })).not.toBeInTheDocument()
  })

  it('returns to the requested route after signing in', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ status: 'success' })))
    render(
      <MemoryRouter initialEntries={[{
        pathname: '/employee/login',
        state: { from: { pathname: '/employee/dashboard', search: '?tab=metadata', hash: '#top' } },
      }]}
      >
        <AuthContext.Provider value={{ employee: anonymousEmployee, refreshEmployee: vi.fn() }}>
          <Routes>
            <Route path="employee/login" element={<EmployeeLoginPage />} />
            <Route path="employee/dashboard" element={<LocationProbe />} />
          </Routes>
        </AuthContext.Provider>
      </MemoryRouter>,
    )
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Username'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Location: /employee/dashboard?tab=metadata#top'))
      .toBeInTheDocument()
  })
})
