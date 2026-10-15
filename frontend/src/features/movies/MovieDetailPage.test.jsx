import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import CartStatusProvider from '../cart/CartStatusProvider.jsx'
import MovieDetailPage from './MovieDetailPage.jsx'

// The add-to-cart control uses the shared cart status; an anonymous session
// keeps the provider from requesting the cart count on mount.
const anonymousSession = { status: 'anonymous', user: null, error: null }

function movieDetail(overrides = {}) {
  return {
    movie_id: 'tt9000001',
    movie_title: 'Neon Horizon',
    movie_year: 2021,
    movie_director: 'Avery Stone',
    movie_genres: 'Action, Science Fiction',
    movie_rating: '8.4',
    movie_stars: [
      { star_id: 'nm9000001', star_name: 'Alex Rivera' },
      { star_id: 'nm9000002', star_name: 'Jamie Park' },
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

function renderMovieDetail(path, fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <AuthContext.Provider value={{ customer: anonymousSession }}>
      <CartStatusProvider>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="movies/:movieId" element={<MovieDetailPage />} />
            <Route path="movies" element={<p>Movie list</p>} />
            <Route path="stars/:starId" element={<p>Star detail</p>} />
          </Routes>
        </MemoryRouter>
      </CartStatusProvider>
    </AuthContext.Provider>,
  )
}

afterEach(() => {
  window.sessionStorage.clear()
  vi.unstubAllGlobals()
})

describe('MovieDetailPage', () => {
  it('requests the movie by its route id and renders the metadata', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(movieDetail()))
    renderMovieDetail('/movies/tt9000001', fetchMock)

    expect(await screen.findByRole('heading', { name: 'Neon Horizon' })).toBeInTheDocument()

    expect(fetchMock).toHaveBeenCalledWith('/api/boot/single-movie?id=tt9000001', expect.anything())
    expect(screen.getByText('2021')).toBeInTheDocument()
    expect(screen.getByText('Avery Stone')).toBeInTheDocument()
    expect(screen.getByText('8.4')).toBeInTheDocument()
  })

  it('links genres back to the movie list browse', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(movieDetail()))
    renderMovieDetail('/movies/tt9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Neon Horizon' })

    expect(screen.getByRole('link', { name: 'Action' })).toHaveAttribute(
      'href',
      '/movies?genre=Action&pageNumber=0&pageSize=10',
    )
    expect(screen.getByRole('link', { name: 'Science Fiction' })).toHaveAttribute(
      'href',
      '/movies?genre=Science+Fiction&pageNumber=0&pageSize=10',
    )
  })

  it('links stars to the star detail route', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(movieDetail()))
    renderMovieDetail('/movies/tt9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Neon Horizon' })

    expect(screen.getByRole('link', { name: 'Alex Rivera' })).toHaveAttribute(
      'href',
      '/stars/nm9000001',
    )
    expect(screen.getByRole('link', { name: 'Jamie Park' })).toHaveAttribute(
      'href',
      '/stars/nm9000002',
    )
  })

  it('renders N/A fallbacks for missing genres and placeholder stars', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(movieDetail({
      movie_genres: '',
      movie_rating: 'N/A',
      movie_stars: [{ star_id: '', star_name: 'N/A' }],
    })))
    renderMovieDetail('/movies/tt9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Neon Horizon' })

    expect(screen.getAllByText('N/A')).toHaveLength(3)
  })

  it('renders a not-found state for unknown movie ids', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ errorMessage: 'Movie not found.' }, 404),
    )
    renderMovieDetail('/movies/tt0000000', fetchMock)

    expect(await screen.findByRole('heading', { name: 'Movie not found' })).toBeInTheDocument()
    expect(screen.getByText('No Fabflix movie matches “tt0000000”.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Try again' })).not.toBeInTheDocument()
  })

  it('shows a retryable error when the request fails', async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockResolvedValue(jsonResponse(movieDetail()))
    renderMovieDetail('/movies/tt9000001', fetchMock)
    const user = userEvent.setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('heading', { name: 'Neon Horizon' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('returns to the movie list with the stored list query', async () => {
    window.sessionStorage.setItem('fabflix.lastMovieQuery', 'genre=Action&pageNumber=3&pageSize=25')
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(movieDetail()))
    renderMovieDetail('/movies/tt9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Neon Horizon' })

    expect(screen.getByRole('link', { name: 'Back to movie list' })).toHaveAttribute(
      'href',
      '/movies?genre=Action&pageNumber=3&pageSize=25',
    )
  })

  it('returns to the plain movie list without stored state', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(movieDetail()))
    renderMovieDetail('/movies/tt9000001', fetchMock)

    await screen.findByRole('heading', { name: 'Neon Horizon' })

    expect(screen.getByRole('link', { name: 'Back to movie list' })).toHaveAttribute(
      'href',
      '/movies',
    )
  })

  it('adds the movie to the cart from the detail page', async () => {
    const fetchMock = vi.fn((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/boot/cart')) {
        return Promise.resolve(jsonResponse({ status: 'success' }))
      }
      return Promise.resolve(jsonResponse(movieDetail()))
    })
    renderMovieDetail('/movies/tt9000001', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('heading', { name: 'Neon Horizon' })
    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    const cartCall = fetchMock.mock.calls.find(([url]) => String(url).includes('/boot/cart'))
    expect(String(cartCall[0])).toBe('/api/boot/cart')
    expect(String(cartCall[1].body)).toBe('movieId=tt9000001&action=add')
  })
})
