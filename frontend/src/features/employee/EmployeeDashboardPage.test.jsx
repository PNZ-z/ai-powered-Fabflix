import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import { expectNoA11yViolations } from '../../test/axe.js'
import EmployeeDashboardPage from './EmployeeDashboardPage.jsx'

const authenticatedEmployee = { status: 'authenticated', user: {}, error: null }

const databaseMetadata = {
  status: 'success',
  tables: [{
    table_name: 'movies',
    columns: [
      { column_name: 'id', column_type: 'varchar(10)' },
      { column_name: 'title', column_type: 'varchar(100)' },
    ],
  }],
}

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

// Routes each dashboard request to a canned response so a test can vary one
// endpoint without losing the others.
function dashboardFetch(handlers = {}) {
  return vi.fn((input) => {
    const url = String(input)
    if (url.includes('action=metadata')) {
      return Promise.resolve(handlers.metadata ?? jsonResponse(databaseMetadata))
    }
    if (url.includes('action=add-star')) {
      return Promise.resolve(handlers.addStar ?? jsonResponse({
        status: 'success',
        message: 'Star inserted successfully. New star ID: nm0000009',
      }))
    }
    if (url.includes('action=add-movie')) {
      return Promise.resolve(handlers.addMovie ?? jsonResponse({
        status: 'success',
        message: 'Movie added successfully.',
      }))
    }
    if (url.includes('/boot/dashboard/logout')) {
      return Promise.resolve(handlers.logout ?? new Response(null, { status: 204 }))
    }
    return Promise.resolve(jsonResponse({}))
  })
}

function renderDashboard(fetchMock, clearEmployee = vi.fn()) {
  vi.stubGlobal('fetch', fetchMock)
  const user = userEvent.setup()
  render(
    <MemoryRouter initialEntries={['/employee/dashboard']}>
      <AuthContext.Provider value={{ employee: authenticatedEmployee, clearEmployee }}>
        <Routes>
          <Route path="employee/dashboard" element={<EmployeeDashboardPage />} />
          <Route path="employee/login" element={<h1>Employee login</h1>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
  return { user, clearEmployee }
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('EmployeeDashboardPage', () => {
  it('renders each metadata table as a card with column name and type', async () => {
    const fetchMock = dashboardFetch({
      metadata: jsonResponse({
        status: 'success',
        tables: [
          databaseMetadata.tables[0],
          { table_name: 'stars', columns: [{ column_name: 'name', column_type: 'int' }] },
        ],
      }),
    })
    renderDashboard(fetchMock)

    expect(await screen.findByRole('heading', { name: 'movies' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'stars' })).toBeInTheDocument()
    expect(screen.getAllByRole('columnheader', { name: 'Column Name' })).toHaveLength(2)
    expect(screen.getAllByRole('columnheader', { name: 'Column Type' })).toHaveLength(2)
    expect(screen.getByText('title')).toBeInTheDocument()
    expect(screen.getByText('varchar(100)')).toBeInTheDocument()
    // The metadata section renders inline states, so the page keeps one <h1>.
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1)
  })

  it('shows the empty metadata state while keeping the management forms', async () => {
    const fetchMock = dashboardFetch({
      metadata: jsonResponse({ status: 'success', tables: [] }),
    })
    renderDashboard(fetchMock)

    expect(await screen.findByText('No metadata found.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Insert star' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add movie' })).toBeInTheDocument()
  })

  it('marks the legacy-required fields and keeps the optional ones optional', async () => {
    const fetchMock = dashboardFetch()
    renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    expect(screen.getByLabelText('Star name')).toBeRequired()
    expect(screen.getByLabelText('Birth year (optional)')).not.toBeRequired()
    expect(screen.getByLabelText('Movie title')).toBeRequired()
    expect(screen.getByLabelText('Movie year')).toBeRequired()
    expect(screen.getByLabelText('Movie director')).toBeRequired()
    expect(screen.getByLabelText('Movie star name')).toBeRequired()
    expect(screen.getByLabelText('Movie star birth year (optional)')).not.toBeRequired()
    expect(screen.getByLabelText('Movie genre name')).toBeRequired()
  })

  it('retries a failed metadata request', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ status: 'fail', message: 'Metadata unavailable.' }, 500))
      .mockResolvedValueOnce(jsonResponse(databaseMetadata))
    const { user } = renderDashboard(fetchMock)

    expect(await screen.findByRole('alert')).toHaveTextContent('Metadata unavailable.')

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: 'movies' })).toBeInTheDocument()
  })

  it('clears the employee session and returns to login when metadata is unauthorized', async () => {
    const fetchMock = dashboardFetch({
      metadata: jsonResponse({ status: 'fail', message: 'Employee authentication required.' }, 401),
    })
    const { clearEmployee } = renderDashboard(fetchMock)

    expect(await screen.findByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
    expect(clearEmployee).toHaveBeenCalledOnce()
  })

  it('submits add-star, shows the success message, and clears the form', async () => {
    const fetchMock = dashboardFetch()
    const { user } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.type(screen.getByLabelText('Star name'), 'Ada Lovelace')
    await user.type(screen.getByLabelText('Birth year (optional)'), '1815')
    await user.click(screen.getByRole('button', { name: 'Insert star' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Star inserted successfully')
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes('action=add-star'))
    expect(String(call[1].body)).toBe('star_name=Ada+Lovelace&birth_year=1815')
    expect(screen.getByLabelText('Star name')).toHaveValue('')
  })

  it('shows the add-star failure message and keeps the input', async () => {
    const fetchMock = dashboardFetch({
      addStar: jsonResponse({ status: 'fail', message: 'Star name cannot be empty.' }),
    })
    const { user } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.type(screen.getByLabelText('Star name'), 'Ada')
    await user.click(screen.getByRole('button', { name: 'Insert star' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Star name cannot be empty.')
    expect(screen.getByLabelText('Star name')).toHaveValue('Ada')
  })

  it('submits add-movie with every form field', async () => {
    const fetchMock = dashboardFetch()
    const { user } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.type(screen.getByLabelText('Movie title'), 'Neon Horizon')
    await user.type(screen.getByLabelText('Movie year'), '2024')
    await user.type(screen.getByLabelText('Movie director'), 'Ada')
    await user.type(screen.getByLabelText('Movie star name'), 'Grace')
    await user.type(screen.getByLabelText('Movie genre name'), 'Drama')
    await user.click(screen.getByRole('button', { name: 'Add movie' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Movie added successfully.')
    const call = fetchMock.mock.calls.find(([input]) => String(input).includes('action=add-movie'))
    expect(String(call[1].body)).toBe(
      'movie_title=Neon+Horizon&movie_year=2024&movie_director=Ada'
      + '&movie_star_name=Grace&movie_star_birth_year=&movie_genre_name=Drama',
    )
  })

  it('treats an add-movie business failure as a failure even with a 200 status', async () => {
    const fetchMock = dashboardFetch({
      addMovie: jsonResponse({ status: 'fail', message: 'Movie already exists: Neon Horizon' }),
    })
    const { user } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    // The legacy-required fields must be filled before the browser submits.
    await user.type(screen.getByLabelText('Movie title'), 'Neon Horizon')
    await user.type(screen.getByLabelText('Movie year'), '2024')
    await user.type(screen.getByLabelText('Movie director'), 'Ada')
    await user.type(screen.getByLabelText('Movie star name'), 'Grace')
    await user.type(screen.getByLabelText('Movie genre name'), 'Drama')
    await user.click(screen.getByRole('button', { name: 'Add movie' }))

    expect(await screen.findByRole('alert'))
      .toHaveTextContent('Movie already exists: Neon Horizon')
  })

  it('signs out through the dashboard and returns to login', async () => {
    const fetchMock = dashboardFetch()
    const { user, clearEmployee } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
    expect(clearEmployee).toHaveBeenCalledOnce()
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/boot/dashboard/logout')))
      .toBe(true)
  })

  it('keeps the employee signed in when logout fails', async () => {
    const fetchMock = dashboardFetch({
      logout: jsonResponse({ status: 'fail', message: 'Employee authentication required.' }, 401),
    })
    const { user, clearEmployee } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Employee authentication required.')
    expect(clearEmployee).not.toHaveBeenCalled()
    expect(screen.getByRole('heading', { name: 'Employee dashboard' })).toBeInTheDocument()
  })

  it('has no detectable accessibility violations', async () => {
    const fetchMock = dashboardFetch()
    renderDashboard(fetchMock)

    await screen.findByRole('heading', { name: 'movies' })
    await expectNoA11yViolations()
  })

  it('clears the employee session and returns to login when add-star is unauthorized', async () => {
    const fetchMock = dashboardFetch({
      addStar: jsonResponse({ status: 'fail', message: 'Employee authentication required.' }, 401),
    })
    const { user, clearEmployee } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.type(screen.getByLabelText('Star name'), 'Ada')
    await user.click(screen.getByRole('button', { name: 'Insert star' }))

    expect(await screen.findByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
    expect(clearEmployee).toHaveBeenCalledOnce()
  })

  it('clears the employee session and returns to login when add-movie is unauthorized', async () => {
    const fetchMock = dashboardFetch({
      addMovie: jsonResponse({ status: 'fail', message: 'Employee authentication required.' }, 401),
    })
    const { user, clearEmployee } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.type(screen.getByLabelText('Movie title'), 'Neon Horizon')
    await user.type(screen.getByLabelText('Movie year'), '2024')
    await user.type(screen.getByLabelText('Movie director'), 'Ada')
    await user.type(screen.getByLabelText('Movie star name'), 'Grace')
    await user.type(screen.getByLabelText('Movie genre name'), 'Drama')
    await user.click(screen.getByRole('button', { name: 'Add movie' }))

    expect(await screen.findByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
    expect(clearEmployee).toHaveBeenCalledOnce()
  })

  it('shows a safe message and keeps the input when add-star cannot reach the server', async () => {
    const fetchMock = vi.fn((input) => {
      const url = String(input)
      if (url.includes('action=metadata')) return Promise.resolve(jsonResponse(databaseMetadata))
      if (url.includes('action=add-star')) return Promise.reject(new TypeError('offline'))
      return Promise.resolve(jsonResponse({}))
    })
    const { user, clearEmployee } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.type(screen.getByLabelText('Star name'), 'Ada')
    await user.click(screen.getByRole('button', { name: 'Insert star' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    expect(screen.getByLabelText('Star name')).toHaveValue('Ada')
    expect(clearEmployee).not.toHaveBeenCalled()
  })

  it('shows a safe message and keeps the inputs when add-movie cannot reach the server', async () => {
    const fetchMock = vi.fn((input) => {
      const url = String(input)
      if (url.includes('action=metadata')) return Promise.resolve(jsonResponse(databaseMetadata))
      if (url.includes('action=add-movie')) return Promise.reject(new TypeError('offline'))
      return Promise.resolve(jsonResponse({}))
    })
    const { user, clearEmployee } = renderDashboard(fetchMock)
    await screen.findByRole('heading', { name: 'movies' })

    await user.type(screen.getByLabelText('Movie title'), 'Neon Horizon')
    await user.type(screen.getByLabelText('Movie year'), '2024')
    await user.type(screen.getByLabelText('Movie director'), 'Ada')
    await user.type(screen.getByLabelText('Movie star name'), 'Grace')
    await user.type(screen.getByLabelText('Movie genre name'), 'Drama')
    await user.click(screen.getByRole('button', { name: 'Add movie' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    expect(screen.getByLabelText('Movie title')).toHaveValue('Neon Horizon')
    expect(clearEmployee).not.toHaveBeenCalled()
  })
})
