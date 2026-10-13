import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import LogoutButton from './LogoutButton.jsx'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('LogoutButton', () => {
  it('invalidates the backend session and clears customer state', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    const clearCustomer = vi.fn()
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/home']}>
        <AuthContext.Provider value={{ clearCustomer }}>
          <Routes>
            <Route path="home" element={<LogoutButton />} />
            <Route path="login" element={<h1>Customer login</h1>} />
          </Routes>
        </AuthContext.Provider>
      </MemoryRouter>,
    )

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('heading', { name: 'Customer login' })).toBeInTheDocument()
    expect(clearCustomer).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledWith('/api/boot/logout', expect.objectContaining({
      credentials: 'include',
    }))
  })
})
