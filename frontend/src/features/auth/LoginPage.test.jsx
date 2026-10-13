import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import LoginPage from './LoginPage.jsx'

const anonymousCustomer = { status: 'anonymous', user: null, error: null }

function renderLogin(refreshCustomer = vi.fn().mockResolvedValue(undefined)) {
  const auth = { customer: anonymousCustomer, refreshCustomer }
  render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route path="login" element={<LoginPage />} />
          <Route path="home" element={<h1>Customer home</h1>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
  return refreshCustomer
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('LoginPage', () => {
  it('logs in, refreshes the session, and navigates home', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 'success' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetchMock)
    const refreshCustomer = renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Email'), 'viewer@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Customer home' })).toBeInTheDocument()
    expect(refreshCustomer).toHaveBeenCalledOnce()
    const request = fetchMock.mock.calls[0]
    expect(request[0]).toBe('/api/boot/login')
    expect(request[1].method).toBe('POST')
    expect(request[1].body.get('username')).toBe('viewer@example.com')
    expect(request[1].body.get('password')).toBe('secret')
    expect(request[1].credentials).toBe('include')
  })

  it('shows the legacy failure message without navigating', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 'fail', message: 'Invalid username or password' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))
    renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Email'), 'viewer@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password')
    expect(screen.getByRole('heading', { name: 'Sign in to Fabflix' })).toBeInTheDocument()
  })

  it('shows normalized request failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('offline')))
    renderLogin()
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Email'), 'viewer@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
  })
})
