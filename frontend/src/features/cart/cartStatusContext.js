import { createContext, useContext } from 'react'

const CartStatusContext = createContext(null)

function useCartStatus() {
  const context = useContext(CartStatusContext)
  if (!context) {
    throw new Error('useCartStatus must be used inside CartStatusProvider')
  }

  return context
}

export { CartStatusContext, useCartStatus }
