import { Navigate, Outlet, useLocation } from 'react-router-dom'
import ErrorState from '../components/states/ErrorState.jsx'
import LoadingState from '../components/states/LoadingState.jsx'
import { useAuth } from './authContext.js'

function ProtectedRoute({ audience }) {
  const auth = useAuth()
  const location = useLocation()
  const session = auth[audience]
  const retry = audience === 'employee' ? auth.refreshEmployee : auth.refreshCustomer
  const loginPath = audience === 'employee' ? '/employee/login' : '/login'

  if (session.status === 'checking') {
    return <LoadingState message={`Checking ${audience} session...`} />
  }

  if (session.status === 'error') {
    return <ErrorState message={session.error.message} onRetry={retry} />
  }

  if (session.status !== 'authenticated') {
    return <Navigate to={loginPath} replace state={{ from: location }} />
  }

  return <Outlet />
}

export default ProtectedRoute
