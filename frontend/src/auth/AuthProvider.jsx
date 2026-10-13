import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiRequest } from '../api/apiClient.js'
import { AuthContext } from './authContext.js'

const anonymousSession = { status: 'anonymous', user: null, error: null }
const checkingSession = { status: 'checking', user: null, error: null }

async function loadSession(path) {
  try {
    const user = await apiRequest(path)
    return { status: 'authenticated', user, error: null }
  } catch (error) {
    if (error.isUnauthorized) {
      return anonymousSession
    }

    return { status: 'error', user: null, error }
  }
}

function AuthProvider({ children }) {
  const [customer, setCustomer] = useState(checkingSession)
  const [employee, setEmployee] = useState(checkingSession)

  const refreshCustomer = useCallback(async () => {
    setCustomer(checkingSession)
    setCustomer(await loadSession('/boot/session/customer'))
  }, [])

  const refreshEmployee = useCallback(async () => {
    setEmployee(checkingSession)
    setEmployee(await loadSession('/boot/dashboard/session'))
  }, [])

  useEffect(() => {
    let active = true

    loadSession('/boot/session/customer').then((session) => {
      if (active) setCustomer(session)
    })
    loadSession('/boot/dashboard/session').then((session) => {
      if (active) setEmployee(session)
    })

    return () => {
      active = false
    }
  }, [])

  const value = useMemo(() => ({
    customer,
    employee,
    refreshCustomer,
    refreshEmployee,
    setCustomerAuthenticated: (user) => setCustomer({ status: 'authenticated', user, error: null }),
    setEmployeeAuthenticated: (user) => setEmployee({ status: 'authenticated', user, error: null }),
    clearCustomer: () => setCustomer(anonymousSession),
    clearEmployee: () => setEmployee(anonymousSession),
  }), [customer, employee, refreshCustomer, refreshEmployee])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default AuthProvider
