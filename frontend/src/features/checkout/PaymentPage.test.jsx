import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../test/axe.js'
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

function ConfirmationProbe() {
  const location = useLocation()
  const total = location.state?.order?.total_amount
  return <p>Confirmation order total {total ?? 'none'}</p>
}

function renderCheckout(fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <MemoryRouter initialEntries={['/checkout']}>
      <Routes>
        <Route path="checkout" element={<PaymentPage />} />
        <Route path="cart" element={<p>Cart page</p>} />
        <Route path="movies" element={<p>Movies page</p>} />
        <Route path="confirmation" element={<ConfirmationProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

// Dispatches the mocked responses by request URL so the parallel cart and
// token reads do not collide.
function fetchRouter({
  cart = cartItems,
  token = { status: 'success', token: 'tok-1' },
  tokenStatus = 200,
  payment = { status: 'success', message: 'Order placed successfully.', order: placedOrder },
} = {}) {
  return vi.fn((input) => {
    const url = typeof input === 'string' ? input : input.url
    if (url.includes('/boot/cart')) return Promise.resolve(jsonResponse(cart))
    if (url.includes('/boot/checkout/token')) {
      return Promise.resolve(jsonResponse(token, tokenStatus))
    }
    if (url.includes('/boot/payment')) return Promise.resolve(jsonResponse(payment))
    return Promise.resolve(jsonResponse({}))
  })
}

function fillForm(cardNumber = '1234') {
  fireEvent.change(screen.getByLabelText('First name'), { target: { value: 'Ada' } })
  fireEvent.change(screen.getByLabelText('Last name'), { target: { value: 'Lovelace' } })
  fireEvent.change(screen.getByLabelText('Credit card number'), {
    target: { value: cardNumber },
  })
  fireEvent.change(screen.getByLabelText('Expiration date'), {
    target: { value: '2030-01-01' },
  })
}

function paymentCalls(fetchMock) {
  return fetchMock.mock.calls.filter(([url]) => String(url).includes('/boot/payment'))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('PaymentPage', () => {
  it('renders required payment fields with a date expiration', async () => {
    renderCheckout(fetchRouter())

    await screen.findByRole('heading', { name: 'Checkout' })

    expect(screen.getByLabelText('First name')).toBeRequired()
    expect(screen.getByLabelText('Last name')).toBeRequired()
    expect(screen.getByLabelText('Credit card number')).toBeRequired()
    const expiration = screen.getByLabelText('Expiration date')
    expect(expiration).toBeRequired()
    expect(expiration).toHaveAttribute('type', 'date')
  })

  it('submits the form with the checkout token and navigates to confirmation', async () => {
    const fetchMock = fetchRouter()
    renderCheckout(fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('heading', { name: 'Checkout' })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())

    fillForm()
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByText('Confirmation order total 15')).toBeInTheDocument()

    const paymentCall = paymentCalls(fetchMock)[0]
    expect(paymentCall[1].method).toBe('POST')
    expect(String(paymentCall[1].body)).toBe(
      'firstName=Ada&lastName=Lovelace&creditCardNumber=1234'
      + '&expirationDate=2030-01-01&checkoutToken=tok-1',
    )
  })

  it('keeps submit disabled until the checkout token arrives', async () => {
    let resolveToken
    const fetchMock = vi.fn((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/boot/cart')) return Promise.resolve(jsonResponse(cartItems))
      if (url.includes('/boot/checkout/token')) {
        return new Promise((resolve) => { resolveToken = resolve })
      }
      return Promise.resolve(jsonResponse({}))
    })
    renderCheckout(fetchMock)

    await screen.findByRole('heading', { name: 'Checkout' })
    const submit = screen.getByRole('button', { name: 'Place order' })
    expect(submit).toBeDisabled()

    fireEvent.submit(submit.closest('form'))
    expect(paymentCalls(fetchMock)).toHaveLength(0)

    resolveToken(jsonResponse({ status: 'success', token: 'tok-1' }))
    await waitFor(() => expect(submit).toBeEnabled())
  })

  it('shows a safe error and blocks submission when the token cannot be fetched', async () => {
    const fetchMock = fetchRouter({
      token: { errorMessage: 'nope' },
      tokenStatus: 500,
    })
    renderCheckout(fetchMock)

    await screen.findByRole('heading', { name: 'Checkout' })

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'We could not start a secure checkout session.',
    )
    const submit = screen.getByRole('button', { name: 'Place order' })
    expect(submit).toBeDisabled()

    fireEvent.submit(submit.closest('form'))
    expect(paymentCalls(fetchMock)).toHaveLength(0)
  })

  it('submits only once when the form is submitted repeatedly', async () => {
    let resolvePayment
    const fetchMock = vi.fn((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/boot/cart')) return Promise.resolve(jsonResponse(cartItems))
      if (url.includes('/boot/checkout/token')) {
        return Promise.resolve(jsonResponse({ status: 'success', token: 'tok-1' }))
      }
      if (url.includes('/boot/payment')) {
        return new Promise((resolve) => { resolvePayment = resolve })
      }
      return Promise.resolve(jsonResponse({}))
    })
    renderCheckout(fetchMock)

    await screen.findByRole('heading', { name: 'Checkout' })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())

    fillForm()
    const form = screen.getByRole('button', { name: 'Place order' }).closest('form')
    fireEvent.submit(form)
    fireEvent.submit(form)

    expect(paymentCalls(fetchMock)).toHaveLength(1)

    resolvePayment(jsonResponse({ status: 'success', order: placedOrder }))
  })

  it('shows the server failure message without echoing the card number', async () => {
    const fetchMock = fetchRouter({
      payment: { status: 'fail', message: 'Invalid payment information.' },
    })
    renderCheckout(fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('heading', { name: 'Checkout' })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())

    fillForm('4242424242424242')
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Invalid payment information.')
    expect(alert).not.toHaveTextContent('4242')
  })

  it('shows the empty-cart state when the cart is empty', async () => {
    renderCheckout(fetchRouter({ cart: [] }))

    expect(await screen.findByRole('heading', { name: 'Your shopping cart is empty' }))
      .toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to cart' })).toHaveAttribute('href', '/cart')
  })

  it('does not request a checkout token for an empty cart', async () => {
    const fetchMock = fetchRouter({ cart: [] })
    renderCheckout(fetchMock)

    await screen.findByRole('heading', { name: 'Your shopping cart is empty' })

    expect(fetchMock.mock.calls.some(([url]) => String(url).includes('/boot/checkout/token')))
      .toBe(false)
  })

  it('restarts checkout from the failure state and can submit again', async () => {
    let tokenIssued = 0
    let paymentAttempts = 0
    const fetchMock = vi.fn((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/boot/cart')) return Promise.resolve(jsonResponse(cartItems))
      if (url.includes('/boot/checkout/token')) {
        tokenIssued += 1
        return Promise.resolve(jsonResponse({ status: 'success', token: `tok-${tokenIssued}` }))
      }
      if (url.includes('/boot/payment')) {
        paymentAttempts += 1
        if (paymentAttempts === 1) {
          return Promise.resolve(jsonResponse({
            status: 'fail',
            message: 'Your checkout session has expired. Please try again.',
          }))
        }
        return Promise.resolve(jsonResponse({ status: 'success', order: placedOrder }))
      }
      return Promise.resolve(jsonResponse({}))
    })
    renderCheckout(fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('heading', { name: 'Checkout' })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())
    fillForm()
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByRole('alert'))
      .toHaveTextContent('Your checkout session has expired.')
    expect(tokenIssued).toBe(1)

    await user.click(screen.getByRole('button', { name: 'Start checkout again' }))

    // The recovery button reloads the cart and requests a fresh token, clearing
    // the previous error so the visitor can submit without a page reload.
    await waitFor(() => expect(tokenIssued).toBe(2))
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())

    await user.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByText('Confirmation order total 15')).toBeInTheDocument()
    expect(paymentAttempts).toBe(2)
    expect(String(paymentCalls(fetchMock)[1][1].body)).toContain('checkoutToken=tok-2')
  })

  it('has no detectable accessibility violations', async () => {
    renderCheckout(fetchRouter())

    await screen.findByRole('heading', { name: 'Checkout' })
    await waitFor(() => expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled())
    await expectNoA11yViolations()
  })
})
