import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { expectNoA11yViolations } from '../../test/axe.js'
import HomePage from './HomePage.jsx'

function LocationProbe() {
  const location = useLocation()
  return <p>Location: {location.pathname}{location.search}</p>
}

function renderHome(fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  render(
    <MemoryRouter initialEntries={['/home']}>
      <Routes>
        <Route path="home" element={<HomePage />} />
        <Route path="movies" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('HomePage', () => {
  it('renders genre and title browse entry points', async () => {
    renderHome(vi.fn().mockResolvedValue(new Response(
      JSON.stringify([{ name: 'Drama' }, { name: 'Comedy' }]),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))

    expect(await screen.findByRole('link', { name: 'Drama' })).toHaveAttribute(
      'href',
      '/movies?genre=Drama&pageNumber=0&pageSize=10',
    )
    expect(screen.getByRole('link', { name: 'A' })).toHaveAttribute(
      'href',
      '/movies?startsWith=A&pageNumber=0&pageSize=10',
    )
  })

  it('submits advanced search using the legacy query parameters', async () => {
    renderHome(vi.fn().mockResolvedValue(new Response(
      JSON.stringify([]),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('Title'), 'Alien')
    await user.type(screen.getByLabelText('Year'), '1979')
    await user.type(screen.getByLabelText('Director'), 'Ridley Scott')
    await user.click(screen.getByRole('button', { name: 'Search catalog' }))

    expect(screen.getByText(
      'Location: /movies?title=Alien&year=1979&director=Ridley+Scott&pageNumber=0&pageSize=10',
    )).toBeInTheDocument()
  })

  it('shows a retryable error when genres fail to load', async () => {
    renderHome(vi.fn().mockRejectedValue(new TypeError('offline')))

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('has no detectable accessibility violations once genres load', async () => {
    renderHome(vi.fn().mockResolvedValue(new Response(
      JSON.stringify([{ name: 'Drama' }, { name: 'Comedy' }]),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))

    await screen.findByRole('link', { name: 'Drama' })
    await expectNoA11yViolations()
  })
})
