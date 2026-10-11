import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import AppRoutes from './app/AppRoutes.jsx'
import { routeDefinitions } from './app/routeDefinitions.js'

function renderRoute(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

describe('application routing', () => {
  it.each(routeDefinitions)('renders the $title placeholder at $samplePath', (route) => {
    renderRoute(route.samplePath)

    expect(screen.getByRole('heading', { name: route.title })).toBeInTheDocument()
  })

  it('uses client-side navigation from the shared shell', async () => {
    const user = userEvent.setup()
    renderRoute('/home')

    await user.click(screen.getByRole('link', { name: 'Movies' }))

    expect(screen.getByRole('heading', { name: 'Movie catalog' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Movies' })).toHaveAttribute('aria-current', 'page')
  })

  it('redirects the root route to the customer home page', async () => {
    renderRoute('/')

    expect(await screen.findByRole('heading', { name: 'Browse movies' })).toBeInTheDocument()
  })

  it('renders the requested identifier on detail placeholders', () => {
    renderRoute('/movies/tt7654321')

    expect(screen.getByText('Requested ID: tt7654321')).toBeInTheDocument()
  })

  it('renders a recovery link for unknown routes', async () => {
    const user = userEvent.setup()
    renderRoute('/missing-page')

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByText('No Fabflix page matches /missing-page.')).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Return home' }))

    expect(screen.getByRole('heading', { name: 'Browse movies' })).toBeInTheDocument()
  })
})
