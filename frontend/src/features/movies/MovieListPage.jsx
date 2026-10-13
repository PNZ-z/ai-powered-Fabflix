import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import EmptyState from '../../components/states/EmptyState.jsx'
import ErrorState from '../../components/states/ErrorState.jsx'
import LoadingState from '../../components/states/LoadingState.jsx'
import { fetchMovieList } from './movieListApi.js'
import MovieTable from './MovieTable.jsx'

const DEFAULT_PAGE_NUMBER = 0
const DEFAULT_PAGE_SIZE = 10
const PAGE_SIZES = [10, 25, 50, 100]
const SORT_FIELDS = ['title', 'rating']
const SORT_DIRECTIONS = ['ASC', 'DESC']
const OTHER_SORT_FIELD = { title: 'rating', rating: 'title' }

function parseNonNegativeInteger(value, fallback) {
  if (typeof value !== 'string' || value.trim() === '') return fallback
  const parsed = Number.parseInt(value, 10)
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback
}

function parsePositiveInteger(value, fallback) {
  if (typeof value !== 'string' || value.trim() === '') return fallback
  const parsed = Number.parseInt(value, 10)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

// The legacy movie list applies these defaults when the URL carries no
// sorting state, and the backend substitutes the secondary field whenever
// both slots select the same one.
function sortStateFrom(searchParams) {
  const sort1 = searchParams.get('sort1')
  const dir1 = searchParams.get('dir1')
  const sort2 = searchParams.get('sort2')
  const dir2 = searchParams.get('dir2')

  return {
    sort1: SORT_FIELDS.includes(sort1) ? sort1 : 'title',
    dir1: SORT_DIRECTIONS.includes(dir1) ? dir1 : 'ASC',
    sort2: SORT_FIELDS.includes(sort2) ? sort2 : 'rating',
    dir2: SORT_DIRECTIONS.includes(dir2) ? dir2 : 'DESC',
  }
}

// The catalog is fully URL-driven: every filter, sort, and pagination change
// navigates, so remounting on the query string resets all state at once —
// the React equivalent of the legacy full page reload.
function MovieListPage() {
  const [searchParams] = useSearchParams()

  return <MovieCatalog key={searchParams.toString()} />
}

function MovieCatalog() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [sortState, setSortState] = useState(() => sortStateFrom(searchParams))
  const [movies, setMovies] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  const pageNumber = parseNonNegativeInteger(
    searchParams.get('pageNumber'),
    DEFAULT_PAGE_NUMBER,
  )
  const pageSize = parsePositiveInteger(searchParams.get('pageSize'), DEFAULT_PAGE_SIZE)
  const isFullText = searchParams.get('mode') === 'fulltext'
  const query = searchParams.get('query') || ''

  // The backend returns one row beyond the requested page size, so the page
  // can tell whether another page exists before rendering the table.
  useEffect(() => {
    let active = true

    fetchMovieList(searchParams)
      .then((results) => {
        if (!active) return
        setMovies(results)
        setStatus('success')
      })
      .catch((requestError) => {
        if (!active) return
        setError(requestError)
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [searchParams, retryCount])

  function updateParams(mutate) {
    const nextParams = new URLSearchParams(searchParams)
    mutate(nextParams)
    setSearchParams(nextParams)
  }

  function applySort(event) {
    event.preventDefault()
    updateParams((params) => {
      params.set('sort1', sortState.sort1)
      params.set('dir1', sortState.dir1)
      params.set('sort2', sortState.sort2)
      params.set('dir2', sortState.dir2)
    })
  }

  // Selecting the same field twice is not allowed: the other slot flips to
  // the remaining field, exactly like the legacy movie list selects.
  function changeSortField(slot, value) {
    const otherSlot = slot === 'sort1' ? 'sort2' : 'sort1'
    setSortState((current) => ({
      ...current,
      [slot]: value,
      [otherSlot]: OTHER_SORT_FIELD[value],
    }))
  }

  function changeSortDirection(slot, value) {
    setSortState((current) => ({ ...current, [slot]: value }))
  }

  // The legacy page kept the current page number when the result count
  // changed; a page beyond the last results shows the empty state instead.
  function changePageSize(value) {
    const nextSize = Number.parseInt(value, 10)
    if (!Number.isInteger(nextSize) || nextSize <= 0) return
    updateParams((params) => params.set('pageSize', String(nextSize)))
  }

  function goToPage(nextPage) {
    updateParams((params) => params.set('pageNumber', String(Math.max(0, nextPage))))
  }

  function retry() {
    setStatus('loading')
    setError(null)
    setRetryCount((count) => count + 1)
  }

  const displayMovies = movies.slice(0, pageSize)
  const hasNextPage = movies.length > pageSize
  const hasPreviousPage = pageNumber > 0

  return (
    <section className="movie-list-page">
      <p className="route-placeholder__eyebrow">Movie catalog</p>
      <h1>Movie catalog</h1>
      <p className="movie-list-page__context">
        {isFullText
          ? (query ? `Results for “${query}”` : 'Full-text search results')
          : 'Browse and filter the Fabflix catalog.'}
      </p>

      <div className="movie-list-controls">
        <form className="movie-sort" onSubmit={applySort}>
          <label htmlFor="movie-sort-field-1">Sort by</label>
          <select
            id="movie-sort-field-1"
            value={sortState.sort1}
            onChange={(event) => changeSortField('sort1', event.target.value)}
          >
            <option value="title">Title</option>
            <option value="rating">Rating</option>
          </select>
          <label htmlFor="movie-sort-order-1">Order</label>
          <select
            id="movie-sort-order-1"
            value={sortState.dir1}
            onChange={(event) => changeSortDirection('dir1', event.target.value)}
          >
            <option value="ASC">Ascending</option>
            <option value="DESC">Descending</option>
          </select>
          <span className="movie-sort__then" aria-hidden="true">then</span>
          <label htmlFor="movie-sort-field-2">Then by</label>
          <select
            id="movie-sort-field-2"
            value={sortState.sort2}
            onChange={(event) => changeSortField('sort2', event.target.value)}
          >
            <option value="title">Title</option>
            <option value="rating">Rating</option>
          </select>
          <label htmlFor="movie-sort-order-2">Order</label>
          <select
            id="movie-sort-order-2"
            value={sortState.dir2}
            onChange={(event) => changeSortDirection('dir2', event.target.value)}
          >
            <option value="ASC">Ascending</option>
            <option value="DESC">Descending</option>
          </select>
          <button type="submit">Apply</button>
        </form>

        <label className="movie-page-size" htmlFor="movie-page-size">
          Results per page
          <select
            id="movie-page-size"
            value={pageSize}
            onChange={(event) => changePageSize(event.target.value)}
          >
            {PAGE_SIZES.map((size) => (
              <option key={size} value={size}>{size}</option>
            ))}
          </select>
        </label>
      </div>

      {status === 'loading' && <LoadingState message="Loading movies..." />}
      {status === 'error' && <ErrorState message={error.message} onRetry={retry} />}
      {status === 'success' && displayMovies.length === 0 && (
        <EmptyState
          title="No movies found"
          message="Try different filters, or go back to the main page to start a new search."
        />
      )}
      {status === 'success' && displayMovies.length > 0 && (
        <MovieTable movies={displayMovies} pageSize={pageSize} />
      )}

      <div className="pagination" aria-label="Movie pages">
        <button
          type="button"
          disabled={!hasPreviousPage}
          onClick={() => goToPage(pageNumber - 1)}
        >
          Previous
        </button>
        <span>Page {pageNumber + 1}</span>
        <button
          type="button"
          disabled={!hasNextPage}
          onClick={() => goToPage(pageNumber + 1)}
        >
          Next
        </button>
      </div>

      <p className="movie-list-page__back">
        <Link to="/home">Back to main page</Link>
      </p>
    </section>
  )
}

export default MovieListPage
