import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AuthProvider from './AuthProvider.jsx'
import { useAuth } from './authContext.js'

function SessionProbe() {
  const { customer, employee } = useAuth()
  return <p>customer:{customer.status} employee:{employee.status}</p>
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('AuthProvider', () => {
  it('loads customer and employee sessions independently', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url.endsWith('/session/customer')) {
        return Promise.resolve(new Response(
          JSON.stringify({ authenticated: true, role: 'customer', username: 'viewer@example.com' }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        ))
      }

      return Promise.resolve(new Response(
        JSON.stringify({ message: 'Employee authentication required.' }),
        { status: 401, headers: { 'Content-Type': 'application/json' } },
      ))
    }))

    render(<AuthProvider><SessionProbe /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByText('customer:authenticated employee:anonymous')).toBeInTheDocument()
    })
  })

  it('exposes network failures separately from anonymous sessions', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    render(<AuthProvider><SessionProbe /></AuthProvider>)

    await waitFor(() => {
      expect(screen.getByText('customer:error employee:error')).toBeInTheDocument()
    })
  })
})
