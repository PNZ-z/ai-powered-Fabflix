import { apiRequest } from '../../api/apiClient.js'

// Endpoints of the Spring Boot API that back the checkout and confirmation routes.
const checkoutTokenEndpoint = '/boot/checkout/token'
const paymentEndpoint = '/boot/payment'
const confirmationEndpoint = '/boot/confirmation'

// A one-shot checkout token doubles as the idempotency key for the order.
async function fetchCheckoutToken() {
  const result = await apiRequest(checkoutTokenEndpoint)
  return result && result.token ? result.token : null
}

function submitPayment(formValues, checkoutToken) {
  const body = new URLSearchParams({
    firstName: formValues.firstName,
    lastName: formValues.lastName,
    creditCardNumber: formValues.creditCardNumber,
    expirationDate: formValues.expirationDate,
    checkoutToken,
  })
  return apiRequest(paymentEndpoint, { method: 'POST', body })
}

// Returns the most recent successful order, or null when the session has none
// (the endpoint answers 204, which the API client parses as null).
function fetchConfirmation() {
  return apiRequest(confirmationEndpoint)
}

export { fetchCheckoutToken, fetchConfirmation, submitPayment }
