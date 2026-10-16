import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import CartPage from '../cart/CartPage.jsx'
import CartStatusProvider from '../cart/CartStatusProvider.jsx'
import ConfirmationPage from './ConfirmationPage.jsx'
import PaymentPage from './PaymentPage.jsx'

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

const cartItems = [{
  movie_id: 'tt9000001',
  movie_title: 'Neon Horizon',
  quantity: 2,
  price: 7.5,
}]

const placedOrder = {
  order_date: '2026-10-14',
  total_amount: 15,
  lines: [{
    movie_id: 'tt9000001',
    movie_title: 'Neon Horizon',
    quantity: 2,
    unit_price: 7.5,
    line_total: 15,
  }],
}

function flowFetch() {
  return vi.fn((input) => {
    const url = typeof input === 'string' ? input : input.url
    if (url.includes('/boot/checkout/token')) {
      return Promise.resolve(jsonResponse({ status: 'success', token: 'tok-1' }))
    }
    if (url.includes('/boot/payment')) {
      return Promise.resolve(jsonResponse({
        status: 'success',
        message: 'Order placed successfully.',
        order: placedOrder,
      }))
    }
    if (url.includes('/boot/cart')) return Promise.resolve(jsonResponse(cartItems))
    return Promise.resolve(jsonResponse({}))
  })
}

function renderFlow() {
  // The cart status provider is mounted here as an anonymous session, so it adds
  // no /boot/cart request of its own to the request sequence asserted below.
  return render(
    <AuthContext.Provider value={{ customer: { status: 'anonymous', user: null, error: null } }}>
      <CartStatusProvider>
        <MemoryRouter initialEntries={['/cart']}>
          <Routes>
            <Route path="cart" element={<CartPage />} />
            <Route path="checkout" element={<PaymentPage />} />
            <Route path="confirmation" element={<ConfirmationPage />} />
            <Route path="movies" element={<p>Movies page</p>} />
          </Routes>
        </MemoryRouter>
      </CartStatusProvider>
    </AuthContext.Provider>,
  )
}

function fillForm() {
  fireEvent.change(screen.getByLabelText('First name'), { target: { value: 'Ada' } })
  fireEvent.change(screen.getByLabelText('Last name'), { target: { value: 'Lovelace' } })
  fireEvent.change(screen.getByLabelText('Credit card number'), { target: { value: '1234' } })
  fireEvent.change(screen.getByLabelText('Expiration date'), { target: { value: '2030-01-01' } })
}

function requestSequence(fetchMock) {
  return fetchMock.mock.calls.map(([input]) => {
    const url = String(input)
    if (url.includes('/boot/checkout/token')) return 'token'
    if (url.includes('/boot/payment')) return 'payment'
    if (url.includes('/boot/confirmation')) return 'confirmation'
    if (url.includes('/boot/cart')) return 'cart'
    return 'other'
  })
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('checkout flow', () => {
  it('walks cart → checkout → confirmation with the real pages', async () => {
    const fetchMock = flowFetch()
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    renderFlow()

    // /cart: the real cart page loads the session cart.
    await screen.findByRole('heading', { name: 'Shopping cart' })
    expect(screen.getByText('Total: $15.00')).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Proceed to payment' }))

    // /checkout: the real payment page loads the cart then a checkout token.
    await screen.findByRole('heading', { name: 'Checkout' })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())

    fillForm()
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    // /confirmation: the real confirmation page renders the navigated order.
    expect(await screen.findByRole('heading', { name: 'Order confirmed' })).toBeInTheDocument()
    expect(screen.getByText('Neon Horizon')).toBeInTheDocument()
    expect(screen.getByText('Total: $15.00')).toBeInTheDocument()

    // Cross-page request sequence: cart, then (cart + token), then payment.
    // The confirmation page renders from navigation state, so it issues no GET.
    expect(requestSequence(fetchMock)).toEqual(['cart', 'cart', 'token', 'payment'])

    const paymentCall = fetchMock.mock.calls.find(([input]) =>
      String(input).includes('/boot/payment'))
    expect(paymentCall[1].method).toBe('POST')
    expect(String(paymentCall[1].body)).toBe(
      'firstName=Ada&lastName=Lovelace&creditCardNumber=1234'
      + '&expirationDate=2030-01-01&checkoutToken=tok-1',
    )
  })
})
