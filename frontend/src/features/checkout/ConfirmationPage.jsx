import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import EmptyState from '../../components/states/EmptyState.jsx'
import ErrorState from '../../components/states/ErrorState.jsx'
import LoadingState from '../../components/states/LoadingState.jsx'
import { formatCents, toCents } from '../cart/money.js'
import { fetchConfirmation } from './paymentApi.js'

function ConfirmationPage() {
  const location = useLocation()
  // The successful payment navigates here with the order in location state;
  // a refresh or direct visit falls back to the session-backed endpoint.
  const [order, setOrder] = useState(location.state?.order ?? null)
  const [status, setStatus] = useState(location.state?.order ? 'success' : 'loading')
  const [error, setError] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  const hasOrderInState = Boolean(location.state?.order)

  useEffect(() => {
    if (hasOrderInState) return undefined

    let active = true
    fetchConfirmation()
      .then((result) => {
        if (!active) return
        if (result) {
          setOrder(result)
          setStatus('success')
        } else {
          setStatus('empty')
        }
      })
      .catch((requestError) => {
        if (!active) return
        setError(requestError)
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [hasOrderInState, retryCount])

  function retry() {
    setStatus('loading')
    setError(null)
    setRetryCount((count) => count + 1)
  }

  if (status === 'loading') {
    return <LoadingState message="Loading your order..." />
  }

  if (status === 'error') {
    return <ErrorState message={error.message} onRetry={retry} />
  }

  if (status === 'empty' || !order) {
    return (
      <EmptyState
        title="No recent order to display"
        message="Once you place an order, its confirmation appears here."
      >
        <Link className="button-link" to="/movies">Browse movies</Link>
        <Link className="button-link" to="/cart">Back to cart</Link>
      </EmptyState>
    )
  }

  const lines = Array.isArray(order.lines) ? order.lines : []

  return (
    <section className="confirmation-page">
      <p className="route-placeholder__eyebrow">Order confirmation</p>
      <h1>Order confirmed</h1>
      <p>Thank you! Your order has been placed successfully.</p>
      <p className="confirmation-page__date">Order date: {order.order_date}</p>

      <div className="movie-table-wrap">
        <table className="movie-table confirmation-table">
          <thead>
            <tr>
              <th>Movie</th>
              <th>Quantity</th>
              <th>Unit price</th>
              <th>Line total</th>
            </tr>
          </thead>
          <tbody>
            {lines.map((line) => (
              <tr key={line.movie_id}>
                <td>{line.movie_title}</td>
                <td>{line.quantity}</td>
                <td>{formatCents(toCents(line.unit_price))}</td>
                <td>{formatCents(toCents(line.line_total))}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="confirmation-page__total">
        Total: {formatCents(toCents(order.total_amount))}
      </p>

      <p className="confirmation-page__back">
        <Link to="/movies">Continue browsing</Link>
      </p>
    </section>
  )
}

export default ConfirmationPage
