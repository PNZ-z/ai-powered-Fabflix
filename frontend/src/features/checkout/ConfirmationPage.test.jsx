import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../test/axe.js'
import ConfirmationPage from './ConfirmationPage.jsx'

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

const order = {
  order_date: '2026-10-14',
  total_amount: 18.1,
  lines: [
    {
      movie_id: 'tt1',
      movie_title: 'Neon Horizon',
      quantity: 2,
      unit_price: 7.5,
      line_total: 15,
    },
    {
      movie_id: 'tt2',
      movie_title: 'Quiet Signal',
      quantity: 1,
      unit_price: 3.1,
      line_total: 3.1,
    },
  ],
}

function renderConfirmation(fetchMock, { state } = {}) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/confirmation', state }]}>
      <Routes>
        <Route path="confirmation" element={<ConfirmationPage />} />
        <Route path="movies" element={<p>Movies page</p>} />
        <Route path="cart" element={<p>Cart page</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ConfirmationPage', () => {
  it('renders the order passed in navigation state without fetching', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(order))
    renderConfirmation(fetchMock, { state: { order } })

    expect(await screen.findByRole('heading', { name: 'Order confirmed' })).toBeInTheDocument()
    expect(screen.getByText('Order date: 2026-10-14')).toBeInTheDocument()
    expect(screen.getByText('Neon Horizon')).toBeInTheDocument()
    expect(screen.getByText('Quiet Signal')).toBeInTheDocument()
    expect(screen.getByText('$7.50')).toBeInTheDocument()
    expect(screen.getByText('$15.00')).toBeInTheDocument()
    expect(screen.getAllByText('$3.10')).toHaveLength(2)
    expect(screen.getByText('Total: $18.10')).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('loads the most recent order from the confirmation endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(order))
    renderConfirmation(fetchMock)

    expect(await screen.findByRole('heading', { name: 'Order confirmed' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/boot/confirmation', expect.anything())
  })

  it('shows a safe empty state when the session has no order', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    renderConfirmation(fetchMock)

    expect(await screen.findByRole('heading', { name: 'No recent order to display' }))
      .toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse movies' })).toHaveAttribute('href', '/movies')
    expect(screen.getByRole('link', { name: 'Back to cart' })).toHaveAttribute('href', '/cart')
  })

  it('shows a retryable error when the confirmation endpoint fails', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ errorMessage: 'boom' }, 500))
      .mockResolvedValueOnce(jsonResponse(order))
    renderConfirmation(fetchMock)
    const user = userEvent.setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('boom')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: 'Order confirmed' })).toBeInTheDocument()
  })

  it('has no detectable accessibility violations', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(order))
    renderConfirmation(fetchMock, { state: { order } })

    await screen.findByRole('heading', { name: 'Order confirmed' })
    await expectNoA11yViolations()
  })
})
