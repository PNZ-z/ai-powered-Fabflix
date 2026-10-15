import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import AddToCartButton from '../../features/cart/AddToCartButton.jsx'
import CartPage from '../../features/cart/CartPage.jsx'
import AppShell from './AppShell.jsx'

const authenticatedCustomer = {
  status: 'authenticated',
  user: { email: 'viewer@example.com' },
  error: null,
}
const anonymousCustomer = { status: 'anonymous', user: null, error: null }

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

function applyCartAction(cart, movieId, action) {
  if (action === 'add') {
    return [...cart, cartItem({ movie_id: movieId, movie_title: 'Quiet Signal', quantity: 1 })]
  }
  if (action === 'delete') {
    return cart.filter((item) => item.movie_id !== movieId)
  }

  const delta = action === 'increase' ? 1 : -1
  return cart.map((item) => (
    item.movie_id === movieId ? { ...item, quantity: item.quantity + delta } : item
  ))
}

// Minimal /boot/cart server. The badge and the cart page read the same session
// cart, so a mutation made on a page has to be visible in the next count read.
function createCartFetch(items = []) {
  let cart = items

  return vi.fn((url, options = {}) => {
    if (options.method === 'POST') {
      const body = new URLSearchParams(String(options.body))
      cart = applyCartAction(cart, body.get('movieId'), body.get('action'))
      return Promise.resolve(jsonResponse({ status: 'success' }))
    }

    return Promise.resolve(jsonResponse(cart))
  })
}

function AddMoviePage() {
  return <AddToCartButton movieId="tt9000002" movieTitle="Quiet Signal" />
}

function shell(path, customer) {
  const auth = {
    customer,
    employee: anonymousCustomer,
    refreshCustomer: () => {},
    refreshEmployee: () => {},
  }

  return (
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="movies" element={<AddMoviePage />} />
            <Route path="cart" element={<CartPage />} />
          </Route>
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

function renderShell(path, fetchMock, customer = authenticatedCustomer) {
  vi.stubGlobal('fetch', fetchMock)
  return render(shell(path, customer))
}

beforeEach(() => {
  window.localStorage.clear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('AppShell cart badge', () => {
  it('shows the summed quantity of the cart on the Cart link', async () => {
    const fetchMock = createCartFetch([
      cartItem({ quantity: 2 }),
      cartItem({ movie_id: 'tt9000002', movie_title: 'Quiet Signal', quantity: 1 }),
    ])
    renderShell('/movies', fetchMock)

    const cartLink = await screen.findByRole('link', { name: 'Cart, 3 items in your cart' })

    expect(cartLink.querySelector('.cart-badge')).toHaveTextContent('3')
  })

  it('shows "..." from ten pieces on while the accessible name keeps the number', async () => {
    const fetchMock = createCartFetch([
      cartItem({ quantity: 9 }),
      cartItem({ movie_id: 'tt9000002', movie_title: 'Quiet Signal', quantity: 2 }),
    ])
    renderShell('/movies', fetchMock)

    const cartLink = await screen.findByRole('link', { name: 'Cart, 11 items in your cart' })

    expect(cartLink.querySelector('.cart-badge')).toHaveTextContent('...')
  })

  it('renders no badge for an empty cart', async () => {
    const fetchMock = createCartFetch([])
    renderShell('/movies', fetchMock)

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))

    const cartLink = screen.getByRole('link', { name: 'Cart' })
    expect(cartLink.querySelector('.cart-badge')).toBeNull()
  })

  it('counts a successful add on top of the loaded cart', async () => {
    const fetchMock = createCartFetch([cartItem({ quantity: 2 })])
    renderShell('/movies', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('link', { name: 'Cart, 2 items in your cart' })
    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(await screen.findByRole('link', { name: 'Cart, 3 items in your cart' }))
      .toBeInTheDocument()
  })

  it('follows the quantity changes made on the cart page', async () => {
    const fetchMock = createCartFetch([cartItem({ quantity: 2 })])
    renderShell('/cart', fetchMock)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', {
      name: 'Increase quantity for Neon Horizon',
    }))
    expect(await screen.findByRole('link', { name: 'Cart, 3 items in your cart' }))
      .toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Delete Neon Horizon from cart' }))
    expect(await screen.findByRole('link', { name: 'Cart' })).toBeInTheDocument()
  })

  it('still counts an add after the toast is switched off', async () => {
    const fetchMock = createCartFetch([cartItem({ quantity: 2 })])
    renderShell('/movies', fetchMock)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Add to cart' }))
    await user.click(await screen.findByRole('checkbox', { name: "Don't show again" }))
    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(await screen.findByRole('link', { name: 'Cart, 4 items in your cart' }))
      .toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('clears the badge when the cart request reports an expired session', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ errorMessage: 'Authentication required.' }, 401),
    )
    renderShell('/movies', fetchMock)

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))

    const cartLink = screen.getByRole('link', { name: 'Cart' })
    expect(cartLink.querySelector('.cart-badge')).toBeNull()
  })

  it('keeps the last count when a refresh fails', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse([cartItem({ quantity: 3 })]))
      .mockResolvedValueOnce(jsonResponse({ status: 'success' }))
      .mockResolvedValueOnce(jsonResponse({ errorMessage: 'boom' }, 500))
    renderShell('/movies', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('link', { name: 'Cart, 3 items in your cart' })
    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3))
    expect(screen.getByRole('link', { name: 'Cart, 3 items in your cart' })).toBeInTheDocument()
  })

  it('clears the badge when the customer session ends', async () => {
    const fetchMock = createCartFetch([cartItem({ quantity: 2 })])
    vi.stubGlobal('fetch', fetchMock)
    const view = render(shell('/movies', authenticatedCustomer))

    await screen.findByRole('link', { name: 'Cart, 2 items in your cart' })

    view.rerender(shell('/movies', anonymousCustomer))

    expect(screen.getByRole('link', { name: 'Cart' })).toBeInTheDocument()
  })
})
