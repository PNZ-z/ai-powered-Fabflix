import { apiRequest } from '../../api/apiClient.js'

function loginCustomer({ username, password, recaptchaToken = '' }) {
  const body = new URLSearchParams({
    username,
    password,
    'g-recaptcha-response': recaptchaToken,
  })

  return apiRequest('/boot/login', { method: 'POST', body })
}

function logoutCustomer() {
  return apiRequest('/boot/logout')
}

export { loginCustomer, logoutCustomer }
