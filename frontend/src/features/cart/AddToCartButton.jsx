import { useState } from 'react'
import { updateCartItem } from './cartApi.js'

// Single add-to-cart control shared by the movie list and the movie detail
// page. Feedback is inline text instead of the legacy alert() so it is
// reachable and testable.
function AddToCartButton({ movieId, movieTitle }) {
  const [status, setStatus] = useState('idle')
  const [message, setMessage] = useState('')

  async function addToCart() {
    setStatus('pending')
    setMessage('')
    try {
      await updateCartItem(movieId, 'add')
      setStatus('added')
      setMessage(`Added ${movieTitle} to your cart.`)
    } catch (error) {
      setStatus('error')
      setMessage(error.isUnauthorized
        ? 'Please sign in to add movies to your cart.'
        : 'Unable to add this movie to your cart. Please try again.')
    }
  }

  return (
    <span className="add-to-cart">
      <button type="button" disabled={status === 'pending'} onClick={addToCart}>
        Add to cart
      </button>
      {status === 'added' && (
        <span className="add-to-cart__status" role="status">{message}</span>
      )}
      {status === 'error' && (
        <span className="add-to-cart__status" role="alert">{message}</span>
      )}
    </span>
  )
}

export default AddToCartButton
