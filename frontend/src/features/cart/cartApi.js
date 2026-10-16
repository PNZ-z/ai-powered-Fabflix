import { apiRequest } from '../../api/apiClient.js'

// Endpoints of the Spring Boot API that back the cart route.
const cartEndpoint = '/boot/cart'

async function fetchCart() {
  const items = await apiRequest(cartEndpoint)
  return Array.isArray(items) ? items : []
}

// The backend accepts form-encoded movieId/action pairs (legacy shoppingCart.js).
function updateCartItem(movieId, action) {
  const body = new URLSearchParams({ movieId, action })
  return apiRequest(cartEndpoint, { method: 'POST', body })
}

export { fetchCart, updateCartItem }
