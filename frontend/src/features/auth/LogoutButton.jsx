import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/authContext.js'
import { logoutCustomer } from './customerAuthApi.js'

function LogoutButton() {
  const { clearCustomer } = useAuth()
  const navigate = useNavigate()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleLogout() {
    setSubmitting(true)
    setError('')

    try {
      await logoutCustomer()
      clearCustomer()
      navigate('/login', { replace: true })
    } catch (requestError) {
      setError(requestError.message)
      setSubmitting(false)
    }
  }

  return (
    <div className="logout-control">
      <button type="button" disabled={submitting} onClick={handleLogout}>
        {submitting ? 'Signing out...' : 'Sign out'}
      </button>
      {error && <span role="alert">{error}</span>}
    </div>
  )
}

export default LogoutButton
