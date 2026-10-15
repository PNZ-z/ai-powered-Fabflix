import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import EmptyState from '../../components/states/EmptyState.jsx'
import ErrorState from '../../components/states/ErrorState.jsx'
import LoadingState from '../../components/states/LoadingState.jsx'
import { fetchCart, updateCartItem } from './cartApi.js'
import { formatCents, toCents } from './money.js'

function CartPage() {
  const [items, setItems] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [retryCount, setRetryCount] = useState(0)
  const [busyMovieId, setBusyMovieId] = useState(null)

  // The cart lives on the server; every mount re-reads it instead of caching,
  // so navigation and refresh always show the session cart.
  useEffect(() => {
    let active = true

    fetchCart()
      .then((result) => {
        if (!active) return
        setItems(result)
        setStatus('success')
      })
      .catch((requestError) => {
        if (!active) return
        setError(requestError)
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [retryCount])

  async function changeItem(movieId, action) {
    setActionError(null)
    setBusyMovieId(movieId)
    try {
      await updateCartItem(movieId, action)
      setItems(await fetchCart())
      setStatus('success')
    } catch (requestError) {
      setActionError(requestError.message)
    } finally {
      setBusyMovieId(null)
    }
  }

  function retry() {
    setStatus('loading')
    setError(null)
    setRetryCount((count) => count + 1)
  }

  if (status === 'loading') {
    return <LoadingState message="Loading your cart..." />
  }

  if (status === 'error') {
    return <ErrorState message={error.message} onRetry={retry} />
  }

  if (items.length === 0) {
    return (
      <EmptyState
        title="Your shopping cart is empty"
        message="Browse the catalog to add movies to your cart."
      >
        <Link className="button-link" to="/movies">Browse movies</Link>
      </EmptyState>
    )
  }

  const totalCents = items.reduce(
    (sum, item) => sum + toCents(item.price) * item.quantity,
    0,
  )

  return (
    <section className="cart-page">
      <p className="route-placeholder__eyebrow">Shopping cart</p>
      <h1>Shopping cart</h1>

      {actionError && <p className="cart-page__error" role="alert">{actionError}</p>}

      <div className="movie-table-wrap">
        <table className="movie-table cart-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Quantity</th>
              <th>Unit price</th>
              <th>Line total</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.movie_id}>
                <td>{item.movie_title}</td>
                <td>{item.quantity}</td>
                <td>{formatCents(toCents(item.price))}</td>
                <td>{formatCents(toCents(item.price) * item.quantity)}</td>
                <td className="cart-table__actions">
                  <span className="cart-table__actions-group">
                    <button
                      type="button"
                      disabled={busyMovieId === item.movie_id}
                      aria-label={`Increase quantity for ${item.movie_title}`}
                      onClick={() => changeItem(item.movie_id, 'increase')}
                    >
                      +
                    </button>
                    <button
                      type="button"
                      disabled={busyMovieId === item.movie_id}
                      aria-label={`Decrease quantity for ${item.movie_title}`}
                      onClick={() => changeItem(item.movie_id, 'decrease')}
                    >
                      &minus;
                    </button>
                    <button
                      type="button"
                      disabled={busyMovieId === item.movie_id}
                      aria-label={`Delete ${item.movie_title} from cart`}
                      onClick={() => changeItem(item.movie_id, 'delete')}
                    >
                      Delete
                    </button>
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="cart-page__total">Total: {formatCents(totalCents)}</p>

      <p className="cart-page__checkout">
        <Link className="button-link" to="/checkout">Proceed to payment</Link>
      </p>
    </section>
  )
}

export default CartPage
