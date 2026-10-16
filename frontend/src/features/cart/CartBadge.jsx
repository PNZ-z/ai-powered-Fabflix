// The badge has room for a single digit, so ten or more pieces show "...". The
// exact number stays on the Cart link's accessible name (see AppShell).
const CART_BADGE_MAX_DIGITS = 9

function CartBadge({ count }) {
  if (count <= 0) return null

  return (
    <span className="cart-badge" aria-hidden="true">
      {count > CART_BADGE_MAX_DIGITS ? '...' : count}
    </span>
  )
}

export default CartBadge
