import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useCallback, useMemo, useState } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import ProtectedRoute from '../../auth/ProtectedRoute.jsx'
import EmployeeDashboardPage from './EmployeeDashboardPage.jsx'
import EmployeeLoginPage from './EmployeeLoginPage.jsx'

const anonymousEmployee = { status: 'anonymous', user: null, error: null }
const employeeIdentity = {
  authenticated: true,
  role: 'employee',
  username: 'admin@example.com',
  displayName: 'Admin User',
}

const metadata = {
  status: 'success',
  tables: [{
    table_name: 'movies',
    columns: [{ column_name: 'id', column_type: 'varchar(10)' }],
  }],
}

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

function flowFetch() {
  return vi.fn((input) => {
    const url = String(input)
    if (url.includes('/boot/employee-login')) {
      return Promise.resolve(jsonResponse({ status: 'success', message: 'Employee login successful.' }))
    }
    if (url.includes('action=metadata')) return Promise.resolve(jsonResponse(metadata))
    if (url.includes('action=add-star')) {
      return Promise.resolve(jsonResponse({
        status: 'success',
        message: 'Star inserted successfully. New star ID: nm0000009',
      }))
    }
    if (url.includes('/boot/dashboard/logout')) {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    return Promise.resolve(jsonResponse({}))
  })
}

function requestKinds(fetchMock) {
  return fetchMock.mock.calls.map(([input]) => {
    const url = String(input)
    if (url.includes('/boot/employee-login')) return 'employee-login'
    if (url.includes('action=metadata')) return 'metadata'
    if (url.includes('action=add-star')) return 'add-star'
    if (url.includes('/boot/dashboard/logout')) return 'logout'
    return 'other'
  })
}

// A live employee session is needed for the login → dashboard → logout walk,
// so this harness owns the employee state instead of injecting a fixed session.
function AuthHarness({ children }) {
  const [employee, setEmployee] = useState(anonymousEmployee)
  const refreshEmployee = useCallback(async () => {
    setEmployee({ status: 'authenticated', user: employeeIdentity, error: null })
  }, [])
  const clearEmployee = useCallback(() => setEmployee(anonymousEmployee), [])
  const value = useMemo(() => ({
    employee,
    refreshEmployee,
    clearEmployee,
    customer: anonymousEmployee,
  }), [employee, refreshEmployee, clearEmployee])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('employee flow', () => {
  it('walks login → dashboard → metadata → add-star → logout with the real pages', async () => {
    const fetchMock = flowFetch()
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(
      <MemoryRouter initialEntries={['/employee/login']}>
        <AuthHarness>
          <Routes>
            <Route path="employee/login" element={<EmployeeLoginPage />} />
            <Route element={<ProtectedRoute audience="employee" />}>
              <Route path="employee/dashboard" element={<EmployeeDashboardPage />} />
            </Route>
          </Routes>
        </AuthHarness>
      </MemoryRouter>,
    )

    await screen.findByRole('heading', { name: 'Employee login' })
    await user.type(screen.getByLabelText('Username'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    // /employee/dashboard: the real page loads metadata on entry.
    expect(await screen.findByRole('heading', { name: 'Employee dashboard' })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'movies' })).toBeInTheDocument()

    await user.type(screen.getByLabelText('Star name'), 'Ada Lovelace')
    await user.click(screen.getByRole('button', { name: 'Insert star' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Star inserted successfully')

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
    expect(requestKinds(fetchMock)).toEqual(['employee-login', 'metadata', 'add-star', 'logout'])
  })
})
