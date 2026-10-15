import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import CartPage from './CartPage.jsx'

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

function cartItem(overrides = {}) {
  return {
    movie_id: 'tt9000001',
    movie_title: 'Neon Horizon',
    quantity: 2,
    price: 7.5,
    ...overrides,
  }
}

function renderCart(fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <MemoryRouter initialEntries={['/cart']}>
      <Routes>
        <Route path="cart" element={<CartPage />} />
        <Route path="movies" element={<p>Movies page</p>} />
        <Route path="checkout" element={<p>Checkout page</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('CartPage', () => {
  it('renders cart rows and an integer-cent total', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([
      cartItem({ movie_id: 'tt1', movie_title: 'Alpha', quantity: 2, price: 7.5 }),
      cartItem({ movie_id: 'tt2', movie_title: 'Beta', quantity: 1, price: 3.1 }),
    ]))
    renderCart(fetchMock)

    await screen.findByRole('heading', { name: 'Shopping cart' })

    expect(screen.getByText('$7.50')).toBeInTheDocument()
    expect(screen.getByText('$15.00')).toBeInTheDocument()
    expect(screen.getAllByText('$3.10')).toHaveLength(2)
    expect(screen.getByText('Total: $18.10')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Proceed to payment' })).toHaveAttribute(
      'href',
      '/checkout',
    )
  })

  it('posts an increase and re-reads the cart', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse([cartItem()]))
      .mockResolvedValueOnce(jsonResponse({ status: 'success' }))
      .mockResolvedValueOnce(jsonResponse([cartItem({ quantity: 3 })]))
    renderCart(fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('button', { name: 'Increase quantity for Neon Horizon' })
    await user.click(screen.getByRole('button', { name: 'Increase quantity for Neon Horizon' }))

    expect(await screen.findByText('3')).toBeInTheDocument()

    const postCall = fetchMock.mock.calls[1]
    expect(String(postCall[0])).toBe('/api/boot/cart')
    expect(postCall[1].method).toBe('POST')
    expect(String(postCall[1].body)).toBe('movieId=tt9000001&action=increase')
    expect(String(fetchMock.mock.calls[2][0])).toBe('/api/boot/cart')
  })

  it('posts a delete and shows the empty state', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse([cartItem()]))
      .mockResolvedValueOnce(jsonResponse({ status: 'success' }))
      .mockResolvedValueOnce(jsonResponse([]))
    renderCart(fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('button', { name: 'Delete Neon Horizon from cart' })
    await user.click(screen.getByRole('button', { name: 'Delete Neon Horizon from cart' }))

    expect(await screen.findByRole('heading', { name: 'Your shopping cart is empty' }))
      .toBeInTheDocument()
    expect(String(fetchMock.mock.calls[1][1].body)).toBe('movieId=tt9000001&action=delete')
  })

  it('shows the empty state with a browse link', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([]))
    renderCart(fetchMock)

    expect(await screen.findByRole('heading', { name: 'Your shopping cart is empty' }))
      .toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse movies' })).toHaveAttribute('href', '/movies')
  })

  it('shows a retryable error when the cart cannot be loaded', async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockResolvedValue(jsonResponse([cartItem()]))
    renderCart(fetchMock)
    const user = userEvent.setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: 'Shopping cart' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('re-reads the cart on every mount instead of caching', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(jsonResponse([cartItem()])))
    vi.stubGlobal('fetch', fetchMock)

    const first = renderCart(fetchMock)
    await screen.findByRole('heading', { name: 'Shopping cart' })
    first.unmount()

    renderCart(fetchMock)
    await screen.findByRole('heading', { name: 'Shopping cart' })

    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
