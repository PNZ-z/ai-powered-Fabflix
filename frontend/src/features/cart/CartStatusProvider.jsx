import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../../auth/authContext.js'
import { fetchCart } from './cartApi.js'
import { CartStatusContext } from './cartStatusContext.js'

const ADDED_TOAST_DURATION_MS = 2000
const HIDE_ADDED_TOAST_KEY = 'fabflix.hideAddToCartToast'

// The cart page counts pieces, matching the quantity column of GET /boot/cart.
function countCartItems(items) {
  return items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
}

// Returns false when storage is unavailable (private mode), i.e. keep the toast.
function readToastsHidden() {
  try {
    return window.localStorage.getItem(HIDE_ADDED_TOAST_KEY) === 'true'
  } catch {
    return false
  }
}

function persistToastsHidden() {
  try {
    window.localStorage.setItem(HIDE_ADDED_TOAST_KEY, 'true')
  } catch {
    // The preference still applies to this page view.
  }
}

function AddedToCartToast({ message, onClose, onHideForever }) {
  return (
    <div className="cart-toast">
      <p className="cart-toast__message" role="status">{message}</p>
      <button
        type="button"
        className="cart-toast__close"
        aria-label="Close notification"
        onClick={onClose}
      >
        &times;
      </button>
      <label className="cart-toast__opt-out">
        <input type="checkbox" onChange={onHideForever} />
        <span>{"Don't show again"}</span>
      </label>
    </div>
  )
}

function CartStatusProvider({ children }) {
  const { customer } = useAuth()
  // The badge count belongs to the session that produced it: signing out, or in
  // as somebody else, drops the stale value instead of showing it again.
  const sessionKey = customer.status === 'authenticated'
    ? `customer:${customer.user?.email ?? ''}`
    : 'anonymous'
  const [counted, setCounted] = useState({ sessionKey, count: 0 })
  const [countRefresh, setCountRefresh] = useState(0)
  const [addedToast, setAddedToast] = useState(null)
  const [toastsHidden, setToastsHidden] = useState(readToastsHidden)

  // The session cart is the only source of the badge count. It is read when the
  // session is known and whenever something asks for a refresh; every answer
  // that was outrun by a newer read (or arrived after unmount) is dropped.
  useEffect(() => {
    if (customer.status !== 'authenticated') return undefined

    let active = true

    fetchCart()
      .then((items) => {
        if (active) setCounted({ sessionKey, count: countCartItems(items) })
      })
      .catch((error) => {
        // An expired session empties the badge; any other failure keeps the last
        // known count instead of flapping to zero.
        if (active && error.isUnauthorized) setCounted({ sessionKey, count: 0 })
      })

    return () => {
      active = false
    }
  }, [customer.status, sessionKey, countRefresh])

  const requestCountRefresh = useCallback(() => setCountRefresh((count) => count + 1), [])

  const cartCount = counted.sessionKey === sessionKey ? counted.count : 0

  // A fresh object per notification restarts the dismissal timer, so repeated
  // adds update the single toast instead of stacking a second one.
  useEffect(() => {
    if (!addedToast) return undefined

    const timer = window.setTimeout(() => setAddedToast(null), ADDED_TOAST_DURATION_MS)
    return () => window.clearTimeout(timer)
  }, [addedToast])

  // Records a successful add: the badge refreshes even when the customer asked
  // to stop seeing the toast.
  const notifyAdded = useCallback((movieTitle) => {
    requestCountRefresh()
    if (toastsHidden) return

    setAddedToast({ message: `Added "${movieTitle}" to your cart.` })
  }, [requestCountRefresh, toastsHidden])

  return (
    <CartStatusContext.Provider value={{ cartCount, requestCountRefresh, notifyAdded }}>
      {children}
      {addedToast && (
        <AddedToCartToast
          message={addedToast.message}
          onClose={() => setAddedToast(null)}
          onHideForever={() => {
            persistToastsHidden()
            setToastsHidden(true)
            setAddedToast(null)
          }}
        />
      )}
    </CartStatusContext.Provider>
  )
}

export default CartStatusProvider
