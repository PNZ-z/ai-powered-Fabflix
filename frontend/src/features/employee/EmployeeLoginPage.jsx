import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/authContext.js'
import { loginEmployee } from './employeeApi.js'

function EmployeeLoginPage() {
  const { employee, refreshEmployee } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function destinationPath() {
    const requestedPath = location.state?.from
    return requestedPath
      ? `${requestedPath.pathname}${requestedPath.search}${requestedPath.hash}`
      : '/employee/dashboard'
  }

  if (employee.status === 'authenticated') {
    return <Navigate to={destinationPath()} replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setError('')

    try {
      const result = await loginEmployee({ username, password })
      if (result.status !== 'success') {
        setError(result.message || 'Employee login failed.')
        return
      }

      // The login response only carries a status/message, so the employee
      // identity is re-read from the session endpoint instead of being guessed.
      await refreshEmployee()
      navigate(destinationPath(), { replace: true })
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-page">
      <p className="route-placeholder__eyebrow">Employee access</p>
      <h1>Employee login</h1>
      <p>Use your employee account to manage the Fabflix catalog.</p>

      <form className="stack-form" onSubmit={handleSubmit}>
        <label htmlFor="employee-username">Username</label>
        <input
          id="employee-username"
          name="username"
          autoComplete="username"
          required
          value={username}
          onChange={(event) => setUsername(event.target.value)}
        />

        <label htmlFor="employee-password">Password</label>
        <input
          id="employee-password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />

        {error && <p className="form-error" role="alert">{error}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </section>
  )
}

export default EmployeeLoginPage
