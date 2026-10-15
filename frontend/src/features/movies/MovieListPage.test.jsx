import { act, render, screen, waitFor, within } from '@testing-library/react'
import { StrictMode } from 'react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import MovieListPage from './MovieListPage.jsx'
import { MOVIE_LIST_CACHE_TTL_MS, clearMovieListCache, loadMovieList, saveMovieListScroll } from './movieListCache.js'
import { movieListRequestPath } from './movieListApi.js'

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

function deferred() {
  let resolve
  let reject
  const promise = new Promise((settle, fail) => {
    resolve = settle
    reject = fail
  })
  return { promise, resolve, reject }
}

function setScrollY(value) {
  Object.defineProperty(window, 'scrollY', { value, configurable: true, writable: true })
}

// Fills the module cache the way an earlier visit would have, so a test can
// start from a warm list. `ageMs` moves the entry past the TTL for the cases
// that have to observe a background revalidation.
async function seedMovieListCache(query, movies, { ageMs = 0 } = {}) {
  const now = vi.spyOn(Date, 'now').mockReturnValue(Date.now() - ageMs)
  try {
    const key = movieListRequestPath(new URLSearchParams(query))
    await loadMovieList(key, () => Promise.resolve(movies))
  } finally {
    now.mockRestore()
  }
}

function renderMovieList(path, fetchMock, { strict = false } = {}) {
  vi.stubGlobal('fetch', fetchMock)
  const list = (
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="movies" element={<MovieListPage />} />
        <Route path="home" element={<p>Home page</p>} />
      </Routes>
    </MemoryRouter>
  )

  return render(strict ? <StrictMode>{list}</StrictMode> : list)
}

beforeEach(() => {
  clearMovieListCache()
})

afterEach(() => {
  setScrollY(0)
  window.sessionStorage.clear()
  clearMovieListCache()
  vi.restoreAllMocks()
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
    const firstFieldGroup = screen.getByRole('radiogroup', { name: 'Sort by' })
    await user.click(within(firstFieldGroup).getByRole('radio', { name: 'Rating' }))
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(
      within(screen.getByRole('radiogroup', { name: 'Sort by' }))
        .getByRole('radio', { name: 'Rating' }),
    ).toBeChecked()
    expect(
      within(screen.getByRole('radiogroup', { name: 'Then by' }))
        .getByRole('radio', { name: 'Title' }),
    ).toBeChecked()
    expect(requestedUrls(fetchMock)[1]).toBe(
      '/api/boot/movie-list?genre=Drama&sort1=rating&dir1=ASC&sort2=title&dir2=DESC',
    )
  })

  it('keeps the two sort slots on different fields when a pill is clicked', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('link', { name: 'Neon Horizon' })
    const thenGroup = screen.getByRole('radiogroup', { name: 'Then by' })
    await user.click(within(thenGroup).getByText('Rating'))

    expect(within(thenGroup).getByRole('radio', { name: 'Rating' })).toBeChecked()
    expect(
      within(screen.getByRole('radiogroup', { name: 'Sort by' }))
        .getByRole('radio', { name: 'Title' }),
    ).toBeChecked()

    // The pills only change the pending sort; the URL moves on Apply.
    expect(requestedUrls(fetchMock)).toHaveLength(1)

    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(requestedUrls(fetchMock)[1]).toBe(
      '/api/boot/movie-list?sort1=title&dir1=ASC&sort2=rating&dir2=DESC',
    )
  })

  it('keeps both sort fields visible in each slot', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies', fetchMock)

    await screen.findByRole('link', { name: 'Neon Horizon' })

    for (const groupName of ['Sort by', 'Then by']) {
      const group = screen.getByRole('radiogroup', { name: groupName })
      expect(within(group).getByRole('radio', { name: 'Title' })).toBeInTheDocument()
      expect(within(group).getByRole('radio', { name: 'Rating' })).toBeInTheDocument()
    }
  })

  it('restores the sort selection from a shared URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList(
      '/movies?title=neon&sort1=rating&dir1=DESC&sort2=title&dir2=ASC&pageNumber=1&pageSize=25',
      fetchMock,
    )

    await screen.findByRole('link', { name: 'Neon Horizon' })

    expect(
      within(screen.getByRole('radiogroup', { name: 'Sort by' }))
        .getByRole('radio', { name: 'Rating' }),
    ).toBeChecked()
    expect(screen.getByLabelText('Order', { selector: '#movie-sort-order-1' })).toHaveValue('DESC')
    expect(
      within(screen.getByRole('radiogroup', { name: 'Then by' }))
        .getByRole('radio', { name: 'Title' }),
    ).toBeChecked()
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

    // Page one is still cached, so going back renders it without a new request.
    expect(await screen.findByText('Page 1')).toBeInTheDocument()
    expect(requestedUrls(fetchMock)).toHaveLength(2)
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

  it('remembers the list query for the detail pages', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies?genre=Drama&pageNumber=2&pageSize=25', fetchMock)

    await screen.findByRole('link', { name: 'Neon Horizon' })

    expect(window.sessionStorage.getItem('fabflix.lastMovieQuery')).toBe(
      'genre=Drama&pageNumber=2&pageSize=25',
    )
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

  it('adds a movie to the cart from the list', async () => {
    const fetchMock = vi.fn((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/boot/cart')) {
        return Promise.resolve(jsonResponse({ status: 'success' }))
      }
      return Promise.resolve(jsonResponse([movieSummary()]))
    })
    renderMovieList('/movies', fetchMock)
    const user = userEvent.setup()

    await screen.findByRole('link', { name: 'Neon Horizon' })
    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    const cartCall = fetchMock.mock.calls.find(([url]) => String(url).includes('/boot/cart'))
    expect(String(cartCall[0])).toBe('/api/boot/cart')
    expect(cartCall[1].method).toBe('POST')
    expect(String(cartCall[1].body)).toBe('movieId=tt9000001&action=add')
  })
})

describe('MovieListPage caching', () => {
  it('shows the loading state while an uncached list is requested', async () => {
    const pending = deferred()
    const fetchMock = vi.fn(() => pending.promise)
    renderMovieList('/movies?genre=Drama', fetchMock)

    expect(screen.getByRole('status')).toHaveTextContent('Loading movies...')

    pending.resolve(jsonResponse([movieSummary()]))

    expect(await screen.findByRole('link', { name: 'Neon Horizon' })).toBeInTheDocument()
  })

  it('renders a fresh cached list again without requesting it', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    const firstVisit = renderMovieList('/movies?genre=Drama', fetchMock)
    await screen.findByRole('link', { name: 'Neon Horizon' })
    firstVisit.unmount()

    renderMovieList('/movies?genre=Drama', fetchMock)

    // The remembered list is on screen before a request could have answered.
    expect(screen.getByRole('link', { name: 'Neon Horizon' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('renders a stale cached list immediately and revalidates it once', async () => {
    await seedMovieListCache('genre=Drama', [movieSummary({ movie_title: 'Stale Movie' })], {
      ageMs: MOVIE_LIST_CACHE_TTL_MS + 1,
    })
    const pending = deferred()
    const fetchMock = vi.fn(() => pending.promise)

    renderMovieList('/movies?genre=Drama', fetchMock)

    expect(screen.getByRole('link', { name: 'Stale Movie' })).toBeInTheDocument()

    pending.resolve(jsonResponse([movieSummary()]))

    expect(await screen.findByRole('link', { name: 'Neon Horizon' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Stale Movie' })).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('keeps the stale list on screen when the revalidation fails', async () => {
    await seedMovieListCache('genre=Drama', [movieSummary({ movie_title: 'Stale Movie' })], {
      ageMs: MOVIE_LIST_CACHE_TTL_MS + 1,
    })
    const pending = deferred()
    const fetchMock = vi.fn(() => pending.promise)

    renderMovieList('/movies?genre=Drama', fetchMock)

    expect(screen.getByRole('link', { name: 'Stale Movie' })).toBeInTheDocument()

    await act(async () => {
      pending.reject(new TypeError('offline'))
    })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('link', { name: 'Stale Movie' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('bypasses and clears the cache when the visitor retries after an error', async () => {
    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockResolvedValue(jsonResponse([movieSummary()]))
    renderMovieList('/movies?genre=Drama', fetchMock)
    const user = userEvent.setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')

    // Another visit left a list behind for the same query; an explicit retry
    // must still ask the server instead of replaying it.
    await seedMovieListCache('genre=Drama', [movieSummary({ movie_title: 'Cached Movie' })])

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await screen.findByRole('link', { name: 'Neon Horizon' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Cached Movie' })).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('restores the scroll offset of the last visit to the same query', async () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    const firstVisit = renderMovieList('/movies?genre=Drama', fetchMock)
    await screen.findByRole('link', { name: 'Neon Horizon' })

    setScrollY(250)
    firstVisit.unmount()

    renderMovieList('/movies?genre=Drama', fetchMock)

    await waitFor(() => expect(scrollTo).toHaveBeenCalledWith(0, 250))
  })

  it('does not move the page again when a revalidation lands', async () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))
    const firstVisit = renderMovieList('/movies?genre=Drama', fetchMock)
    await screen.findByRole('link', { name: 'Neon Horizon' })

    setScrollY(250)
    firstVisit.unmount()
    await seedMovieListCache('genre=Drama', [movieSummary()], {
      ageMs: MOVIE_LIST_CACHE_TTL_MS + 1,
    })

    const revalidate = vi.fn().mockResolvedValue(
      jsonResponse([movieSummary({ movie_title: 'Revalidated Movie' })]),
    )
    renderMovieList('/movies?genre=Drama', revalidate)

    expect(await screen.findByRole('link', { name: 'Revalidated Movie' })).toBeInTheDocument()
    await waitFor(() => expect(scrollTo).toHaveBeenCalledWith(0, 250))
    expect(scrollTo).toHaveBeenCalledTimes(1)
  })

  it('handles a development double mount without a second request or restore', async () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    saveMovieListScroll(movieListRequestPath(new URLSearchParams('genre=Drama')), 250)
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([movieSummary()]))

    renderMovieList('/movies?genre=Drama', fetchMock, { strict: true })

    expect(await screen.findByRole('link', { name: 'Neon Horizon' })).toBeInTheDocument()
    await waitFor(() => expect(scrollTo).toHaveBeenCalledWith(0, 250))
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(scrollTo).toHaveBeenCalledTimes(1)
  })
})
