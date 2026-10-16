import { useState } from 'react'
import { updateCartItem } from './cartApi.js'
import { useCartStatus } from './cartStatusContext.js'

// Single add-to-cart control shared by the movie list and the movie detail
// page. Success is announced by the shared cart toast; only failures stay next
// to the button so they remain reachable and testable.
function AddToCartButton({ movieId, movieTitle }) {
  const [status, setStatus] = useState('idle')
  const [errorMessage, setErrorMessage] = useState('')
  const { notifyAdded } = useCartStatus()

  async function addToCart() {
    setStatus('pending')
    setErrorMessage('')
    try {
      await updateCartItem(movieId, 'add')
      setStatus('idle')
      notifyAdded(movieTitle)
    } catch (error) {
      setStatus('error')
      setErrorMessage(error.isUnauthorized
        ? 'Please sign in to add movies to your cart.'
        : 'Unable to add this movie to your cart. Please try again.')
    }
  }

  return (
    <span className="add-to-cart">
      <button type="button" disabled={status === 'pending'} onClick={addToCart}>
        Add to cart
      </button>
      {status === 'error' && (
        <span className="add-to-cart__status" role="alert">{errorMessage}</span>
      )}
    </span>
  )
}

export default AddToCartButton
