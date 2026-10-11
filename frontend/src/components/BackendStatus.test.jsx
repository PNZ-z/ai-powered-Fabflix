import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import BackendStatus from './BackendStatus.jsx'

describe('BackendStatus', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the status returned by the backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ status: 'UP' }),
    }))

    render(<BackendStatus />)

    expect(screen.getByText('Backend status: Checking...')).toBeInTheDocument()
    expect(await screen.findByText('Backend status: UP')).toBeInTheDocument()
  })

  it('shows an unavailable state when the request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))

    render(<BackendStatus />)

    expect(await screen.findByText('Backend status: UNAVAILABLE')).toBeInTheDocument()
  })
})
