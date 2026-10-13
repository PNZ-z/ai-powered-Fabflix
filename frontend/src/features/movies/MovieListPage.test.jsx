import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import MovieListPage from './MovieListPage.jsx'

function movieSummary(overrides = {}) {
  return {
    movie_id: 'tt9000001',
    movie_title: 'Neon Horizon',
    movie_year: 2021,
    movie_director: 'Avery Stone',
    movie_genres: 'Action, Drama',
    movie_rating: 8.5,
    movie_stars: [{ star_id: 'nm9000001', star_name: 'Alex Rivera' }],
    ...overrides,
  }
}

function jsonResponse(body) {
  return new Response(
    JSON.stringify(body),
    { status: 200, headers: { 'Content-Type': 'application/json' } },
  )
}

function requestedUrls(fetchMock) {
  return fetchMock.mock.calls.map((call) => String(call[0]))
}

function renderMovieList(path, fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="movies" element={<MovieListPage />} />
        <Route path="home" element={<p>Home page</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('MovieListPage', () => {
  it('requests the browse endpoint with the URL filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies?genre=Drama&pageNumber=2&pageSize=25', fetchMock)

    await screen.findByRole('link', { name: 'Neon Horizon' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/movie-list?genre=Drama&pageNumber=2&pageSize=25',
      expect.anything(),
    )
  })

  it('requests the full-text endpoint in fulltext mode', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies?mode=fulltext&query=neon&pageNumber=0&pageSize=10', fetchMock)

    await screen.findByRole('link', { name: 'Neon Horizon' })

    expect(requestedUrls(fetchMock)[0]).toBe(
      '/api/boot/full-text-search?query=neon&pageNumber=0&pageSize=10',
    )
    expect(screen.getByText('Results for “neon”')).toBeInTheDocument()
  })

  it('renders summaries with detail links and N/A fallbacks', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([
      movieSummary({
        movie_id: 'tt9000002',
        movie_title: 'Quiet Signal',
        movie_genres: '',
        movie_rating: null,
        movie_stars: [{ star_id: '', star_name: 'N/A' }],
      }),
    ]))
    renderMovieList('/movies', fetchMock)

    await screen.findByRole('link', { name: 'Quiet Signal' })

    expect(screen.getByRole('link', { name: 'Quiet Signal' })).toHaveAttribute(
      'href',
      '/movies/tt9000002',
    )
    expect(screen.getAllByText('N/A')).toHaveLength(3)
  })

  it('links genres, stars, and titles to their routes', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([
      movieSummary({
        movie_genres: 'Action, Drama',
        movie_stars: [{ star_id: 'nm9000001', star_name: 'Alex Rivera' }],
      }),
    ]))
    renderMovieList('/movies?pageSize=25', fetchMock)

    await screen.findByRole('link', { name: 'Action' })

    expect(screen.getByRole('link', { name: 'Action' })).toHaveAttribute(
      'href',
      '/movies?genre=Action&pageNumber=0&pageSize=25',
    )
    expect(screen.getByRole('link', { name: 'Alex Rivera' })).toHaveAttribute(
      'href',
      '/stars/nm9000001',
    )
  })

  it('applies the sort selection to the URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies?genre=Drama', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('link', { name: 'Neon Horizon' })
    await user.selectOptions(screen.getByLabelText('Sort by'), 'rating')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(screen.getByLabelText('Then by')).toHaveValue('title')
    expect(requestedUrls(fetchMock)[1]).toBe(
      '/api/boot/movie-list?genre=Drama&sort1=rating&dir1=ASC&sort2=title&dir2=DESC',
    )
  })

  it('restores the sort selection from a shared URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList(
      '/movies?title=neon&sort1=rating&dir1=DESC&sort2=title&dir2=ASC&pageNumber=1&pageSize=25',
      fetchMock,
    )

    await screen.findByRole('link', { name: 'Neon Horizon' })

    expect(screen.getByLabelText('Sort by')).toHaveValue('rating')
    expect(screen.getByLabelText('Order', { selector: '#movie-sort-order-1' })).toHaveValue('DESC')
    expect(screen.getByLabelText('Then by')).toHaveValue('title')
    expect(screen.getByLabelText('Order', { selector: '#movie-sort-order-2' })).toHaveValue('ASC')
    expect(requestedUrls(fetchMock)[0]).toBe(
      '/api/boot/movie-list?title=neon&sort1=rating&dir1=DESC&sort2=title&dir2=ASC&pageNumber=1&pageSize=25',
    )
  })

  it('pages forward and back through the URL', async () => {
    const results = Array.from({ length: 11 }, (_, index) =>
      movieSummary({ movie_id: `tt900000${index}`, movie_title: `Movie ${index}` }))
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(results))
    renderMovieList('/movies?pageNumber=0&pageSize=10', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('link', { name: 'Movie 0' })
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()

    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(requestedUrls(fetchMock)[1]).toBe(
      '/api/boot/movie-list?pageNumber=1&pageSize=10',
    )
    expect(await screen.findByText('Page 2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Previous' }))

    expect(requestedUrls(fetchMock)[2]).toBe(
      '/api/boot/movie-list?pageNumber=0&pageSize=10',
    )
  })

  it('disables Next when the last page is reached', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies?pageNumber=0&pageSize=10', fetchMock)

    await screen.findByRole('link', { name: 'Neon Horizon' })

    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('changes the page size while keeping the page number', async () => {
    const results = Array.from({ length: 11 }, (_, index) =>
      movieSummary({ movie_id: `tt900000${index}`, movie_title: `Movie ${index}` }))
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(results))
    renderMovieList('/movies?pageNumber=1&pageSize=10', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('link', { name: 'Movie 0' })
    await user.selectOptions(screen.getByLabelText('Results per page'), '25')

    expect(requestedUrls(fetchMock)[1]).toBe(
      '/api/boot/movie-list?pageNumber=1&pageSize=25',
    )
  })

  it('shows an empty state when no movies match', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([]))
    renderMovieList('/movies?title=does-not-exist', fetchMock)

    expect(await screen.findByRole('heading', { name: 'No movies found' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('shows a retryable error state when the request fails', async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies', fetchMock)
    const user = userEvent.setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('link', { name: 'Neon Horizon' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
