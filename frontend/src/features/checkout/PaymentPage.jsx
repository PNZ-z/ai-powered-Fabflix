import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import EmptyState from '../../components/states/EmptyState.jsx'
import ErrorState from '../../components/states/ErrorState.jsx'
import LoadingState from '../../components/states/LoadingState.jsx'
import { fetchCart } from '../cart/cartApi.js'
import { formatCents, toCents } from '../cart/money.js'
import { fetchCheckoutToken, submitPayment } from './paymentApi.js'

const EMPTY_FORM = {
  firstName: '',
  lastName: '',
  creditCardNumber: '',
  expirationDate: '',
}

function PaymentPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState([])
  const [cartStatus, setCartStatus] = useState('loading')
  const [cartError, setCartError] = useState(null)
  const [token, setToken] = useState(null)
  const [tokenStatus, setTokenStatus] = useState('loading')
  const [form, setForm] = useState(EMPTY_FORM)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    let active = true

    async function loadCheckout() {
      let cart
      try {
        cart = await fetchCart()
      } catch (requestError) {
        if (!active) return
        setCartError(requestError)
        setCartStatus('error')
        return
      }
      if (!active) return

      setItems(cart)
      if (cart.length === 0) {
        // No point starting a checkout (and issuing a token) for an empty cart.
        setCartStatus('empty')
        return
      }
      setCartStatus('success')

      // A token is requested only once the cart is known to be non-empty;
      // submitting stays disabled until it arrives, so a stale token can never
      // replay an earlier order.
      try {
        const value = await fetchCheckoutToken()
        if (!active) return
        setToken(value)
        setTokenStatus(value ? 'success' : 'error')
      } catch {
        if (!active) return
        setTokenStatus('error')
      }
    }

    loadCheckout()

    return () => {
      active = false
    }
  }, [retryCount])

  function retry() {
    setCartStatus('loading')
    setTokenStatus('loading')
    setToken(null)
    setCartError(null)
    setSubmitError(null)
    setRetryCount((count) => count + 1)
  }

  function updateField(name, value) {
    setForm((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!token || submitting) return

    setSubmitting(true)
    setSubmitError(null)
    try {
      const result = await submitPayment(form, token)
      if (result && result.status === 'success') {
        navigate('/confirmation', { replace: true, state: { order: result.order } })
        return
      }
      setSubmitError(result && result.message
        ? result.message
        : 'Payment failed. Please try again.')
    } catch (error) {
      setSubmitError(error.isUnauthorized
        ? 'Please sign in to complete your payment.'
        : error.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (cartStatus === 'loading') {
    return <LoadingState message="Loading your cart..." />
  }

  if (cartStatus === 'error') {
    return <ErrorState message={cartError.message} onRetry={retry} />
  }

  if (cartStatus === 'empty') {
    return (
      <EmptyState
        title="Your shopping cart is empty"
        message="Add movies to your cart before checking out."
      >
        <Link className="button-link" to="/cart">Back to cart</Link>
        <Link className="button-link" to="/movies">Browse movies</Link>
      </EmptyState>
    )
  }

  const totalCents = items.reduce(
    (sum, item) => sum + toCents(item.price) * item.quantity,
    0,
  )

  return (
    <section className="checkout-page">
      <p className="route-placeholder__eyebrow">Checkout</p>
      <h1>Checkout</h1>
      <p className="checkout-page__total">Total: {formatCents(totalCents)}</p>

      {tokenStatus === 'loading' && (
        <p className="checkout-page__status" role="status">Preparing secure checkout...</p>
      )}
      {tokenStatus === 'error' && (
        <p className="checkout-page__error" role="alert">
          We could not start a secure checkout session. Please try again.
        </p>
      )}
      {tokenStatus === 'error' && (
        <button type="button" onClick={retry}>Try again</button>
      )}

      {submitError && <p className="checkout-page__error" role="alert">{submitError}</p>}

      <form className="checkout-form" onSubmit={handleSubmit}>
        <label htmlFor="payment-first-name">First name</label>
        <input
          id="payment-first-name"
          name="firstName"
          value={form.firstName}
          required
          onChange={(event) => updateField('firstName', event.target.value)}
        />

        <label htmlFor="payment-last-name">Last name</label>
        <input
          id="payment-last-name"
          name="lastName"
          value={form.lastName}
          required
          onChange={(event) => updateField('lastName', event.target.value)}
        />

        <label htmlFor="payment-card-number">Credit card number</label>
        <input
          id="payment-card-number"
          name="creditCardNumber"
          value={form.creditCardNumber}
          required
          onChange={(event) => updateField('creditCardNumber', event.target.value)}
        />

        <label htmlFor="payment-expiration">Expiration date</label>
        <input
          id="payment-expiration"
          name="expirationDate"
          type="date"
          value={form.expirationDate}
          required
          onChange={(event) => updateField('expirationDate', event.target.value)}
        />

        <button type="submit" disabled={submitting || tokenStatus !== 'success'}>
          {submitting ? 'Placing order...' : 'Place order'}
        </button>

        {/* If a submission fails (e.g. the token rotated after a multi-tab or
            StrictMode remount), the user can start a fresh checkout without a
            full page reload. */}
        {submitError && (
          <button type="button" onClick={retry}>Start checkout again</button>
        )}
      </form>

      <p className="checkout-page__back">
        <Link to="/cart">Back to cart</Link>
      </p>
    </section>
  )
}

export default PaymentPage
