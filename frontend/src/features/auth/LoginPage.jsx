import { useCallback, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/authContext.js'
import { loginCustomer } from './customerAuthApi.js'
import RecaptchaField from './RecaptchaField.jsx'

function LoginPage() {
  const { customer, refreshCustomer } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [recaptchaToken, setRecaptchaToken] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [recaptchaReset, setRecaptchaReset] = useState(0)
  const handleRecaptcha = useCallback((token) => setRecaptchaToken(token), [])

  function destinationPath() {
    const requestedPath = location.state?.from
    return requestedPath
      ? `${requestedPath.pathname}${requestedPath.search}${requestedPath.hash}`
      : '/home'
  }

  if (customer.status === 'authenticated') {
    return <Navigate to={destinationPath()} replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')

    try {
      const result = await loginCustomer({ username, password, recaptchaToken })
      if (result.status !== 'success') {
        setError(result.message || 'Login failed.')
        setRecaptchaToken('')
        setRecaptchaReset((value) => value + 1)
        return
      }

      await refreshCustomer()
      navigate(destinationPath(), { replace: true })
    } catch (requestError) {
      setError(requestError.message)
      setRecaptchaToken('')
      setRecaptchaReset((value) => value + 1)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <p className="route-placeholder__eyebrow">Customer access</p>
      <h1>Sign in to Fabflix</h1>
      <p>Use your customer account to browse movies and manage your cart.</p>

      <form className="stack-form" onSubmit={handleSubmit}>
        <label htmlFor="customer-email">Email</label>
        <input
          id="customer-email"
          name="username"
          type="email"
          autoComplete="username"
          required
          value={username}
          onChange={(event) => setUsername(event.target.value)}
        />

        <label htmlFor="customer-password">Password</label>
        <input
          id="customer-password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        <RecaptchaField onTokenChange={handleRecaptcha} resetSignal={recaptchaReset} />
        {error && <p className="form-error" role="alert">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </section>
  )
}

export default LoginPage
