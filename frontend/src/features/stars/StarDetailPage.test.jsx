import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import StarDetailPage from './StarDetailPage.jsx'

function starDetail(overrides = {}) {
  return {
    star_id: 'nm9000001',
    star_name: 'Alex Rivera',
    star_birth_year: 1985,
    star_movies: [
      { movie_id: 'tt9000001', movie_title: 'Neon Horizon' },
      { movie_id: 'tt9000004', movie_title: 'Quiet Signal' },
    ],
    ...overrides,
  }
}

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

function renderStarDetail(path, fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="stars/:starId" element={<StarDetailPage />} />
        <Route path="movies/:movieId" element={<p>Movie detail</p>} />
        <Route path="movies" element={<p>Movie list</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

afterEach(() => {
  window.sessionStorage.clear()
  vi.unstubAllGlobals()
})

describe('StarDetailPage', () => {
  it('requests the star by its route id and renders the metadata', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(starDetail()))
    renderStarDetail('/stars/nm9000001', fetchMock)

    expect(await screen.findByRole('heading', { name: 'Alex Rivera' })).toBeInTheDocument()

    expect(fetchMock).toHaveBeenCalledWith('/api/boot/single-star?id=nm9000001', expect.anything())
    expect(screen.getByText('Birth year')).toBeInTheDocument()
    expect(screen.getByText('1985')).toBeInTheDocument()
  })

  it('links the filmography back to the movie detail route', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(starDetail()))
    renderStarDetail('/stars/nm9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Alex Rivera' })

    expect(screen.getByRole('link', { name: 'Neon Horizon' })).toHaveAttribute(
      'href',
      '/movies/tt9000001',
    )
    expect(screen.getByRole('link', { name: 'Quiet Signal' })).toHaveAttribute(
      'href',
      '/movies/tt9000004',
    )
  })

  it('renders Unknown for a missing birth year and N/A without movies', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(starDetail({
      star_birth_year: null,
      star_movies: [],
    })))
    renderStarDetail('/stars/nm9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Alex Rivera' })

    expect(screen.getByText('Unknown')).toBeInTheDocument()
    expect(screen.getByText('N/A')).toBeInTheDocument()
  })

  it('renders a not-found state for unknown star ids', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ errorMessage: 'Star not found.' }, 404),
    )
    renderStarDetail('/stars/nm0000000', fetchMock)

    expect(await screen.findByRole('heading', { name: 'Star not found' })).toBeInTheDocument()
    expect(screen.getByText('No Fabflix star matches “nm0000000”.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
  })

  it('shows a retryable error when the request fails', async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockResolvedValue(jsonResponse(starDetail()))
    renderStarDetail('/stars/nm9000001', fetchMock)
    const user = userEvent.setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: 'Alex Rivera' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('returns to the movie list with the stored list query', async () => {
    window.sessionStorage.setItem('fabflix.lastMovieQuery', 'mode=fulltext&query=neon&pageNumber=0&pageSize=10')
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(starDetail()))
    renderStarDetail('/stars/nm9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Alex Rivera' })

    expect(screen.getByRole('link', { name: 'Back to movie list' })).toHaveAttribute(
      'href',
      '/movies?mode=fulltext&query=neon&pageNumber=0&pageSize=10',
    )
  })
})
