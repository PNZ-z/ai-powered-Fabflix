import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/authContext.js'
import { addMovie, addStar, fetchMetadata, logoutEmployee } from './employeeApi.js'

const EMPTY_STAR = { starName: '', birthYear: '' }

const EMPTY_MOVIE = {
  movieTitle: '',
  movieYear: '',
  movieDirector: '',
  movieStarName: '',
  movieStarBirthYear: '',
  movieGenreName: '',
}

function ResultMessage({ result }) {
  if (!result) return null

  return (
    <p
      className={result.ok ? 'form-success' : 'form-error'}
      role={result.ok ? 'status' : 'alert'}
    >
      {result.message}
    </p>
  )
}

function EmployeeDashboardPage() {
  const { clearEmployee } = useAuth()
  const navigate = useNavigate()

  const [metadata, setMetadata] = useState([])
  const [metadataStatus, setMetadataStatus] = useState('loading')
  const [metadataError, setMetadataError] = useState(null)
  const [metadataRetryCount, setMetadataRetryCount] = useState(0)

  const [star, setStar] = useState(EMPTY_STAR)
  const [starSubmitting, setStarSubmitting] = useState(false)
  const [starResult, setStarResult] = useState(null)

  const [movie, setMovie] = useState(EMPTY_MOVIE)
  const [movieSubmitting, setMovieSubmitting] = useState(false)
  const [movieResult, setMovieResult] = useState(null)

  const [loggingOut, setLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')

  // The employee session can expire while this page is open; reusing the
  // customer-side behaviour, an unauthorized response clears the local employee
  // state and sends the user back to the login page.
  const endExpiredSession = useCallback((requestError) => {
    if (!requestError.isUnauthorized) return false
    clearEmployee()
    navigate('/employee/login', { replace: true })
    return true
  }, [clearEmployee, navigate])

  useEffect(() => {
    let active = true

    fetchMetadata()
      .then((result) => {
        if (!active) return
        setMetadata(Array.isArray(result?.tables) ? result.tables : [])
        setMetadataStatus('success')
      })
      .catch((requestError) => {
        if (!active) return
        if (endExpiredSession(requestError)) return
        setMetadataError(requestError)
        setMetadataStatus('error')
      })

    return () => {
      active = false
    }
  }, [metadataRetryCount, endExpiredSession])

  function retryMetadata() {
    setMetadataStatus('loading')
    setMetadataError(null)
    setMetadataRetryCount((count) => count + 1)
  }

  function updateStar(field, value) {
    setStar((current) => ({ ...current, [field]: value }))
  }

  function updateMovie(field, value) {
    setMovie((current) => ({ ...current, [field]: value }))
  }

  async function handleAddStar(event) {
    event.preventDefault()
    if (starSubmitting) return

    setStarSubmitting(true)
    setStarResult(null)
    try {
      const result = await addStar(star)
      if (result.status === 'success') {
        setStarResult({ ok: true, message: result.message })
        setStar(EMPTY_STAR)
      } else {
        setStarResult({ ok: false, message: result.message || 'Add star failed.' })
      }
    } catch (requestError) {
      if (!endExpiredSession(requestError)) {
        setStarResult({ ok: false, message: requestError.message })
      }
    } finally {
      setStarSubmitting(false)
    }
  }

  async function handleAddMovie(event) {
    event.preventDefault()
    if (movieSubmitting) return

    setMovieSubmitting(true)
    setMovieResult(null)
    try {
      const result = await addMovie(movie)
      if (result.status === 'success') {
        setMovieResult({ ok: true, message: result.message })
        setMovie(EMPTY_MOVIE)
      } else {
        setMovieResult({ ok: false, message: result.message || 'Add movie failed.' })
      }
    } catch (requestError) {
      if (!endExpiredSession(requestError)) {
        setMovieResult({ ok: false, message: requestError.message })
      }
    } finally {
      setMovieSubmitting(false)
    }
  }

  async function handleLogout() {
    setLoggingOut(true)
    setLogoutError('')
    try {
      await logoutEmployee()
      clearEmployee()
      navigate('/employee/login', { replace: true })
    } catch (requestError) {
      setLogoutError(requestError.message)
      setLoggingOut(false)
    }
  }

  // The metadata area renders inline, non-heading states so the page keeps its
  // single <h1>; the shared state components render page-level headings.
  function renderMetadata() {
    if (metadataStatus === 'loading') {
      return <p role="status">Loading database metadata...</p>
    }

    if (metadataStatus === 'error') {
      return (
        <div className="employee-dashboard__metadata-error">
          <p role="alert">{metadataError.message}</p>
          <button type="button" onClick={retryMetadata}>Try again</button>
        </div>
      )
    }

    if (metadata.length === 0) {
      return <p>No metadata found.</p>
    }

    return (
      <div className="metadata-cards">
        {metadata.map((table) => (
          <div className="metadata-card" key={table.table_name}>
            <h3 className="metadata-card__title">{table.table_name}</h3>
            <table className="metadata-table">
              <thead>
                <tr>
                  <th>Column Name</th>
                  <th>Column Type</th>
                </tr>
              </thead>
              <tbody>
                {table.columns.map((column) => (
                  <tr key={column.column_name}>
                    <td>{column.column_name}</td>
                    <td>{column.column_type}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>
    )
  }

  return (
    <section className="employee-dashboard">
      <p className="route-placeholder__eyebrow">Employee tools</p>
      <h1>Employee dashboard</h1>

      <section className="employee-dashboard__section" aria-label="Database metadata">
        <h2>Database metadata</h2>
        {renderMetadata()}
      </section>

      <section className="employee-dashboard__section" aria-label="Add a star">
        <h2>Add a star</h2>
        <form className="stack-form" onSubmit={handleAddStar}>
          <label htmlFor="employee-star-name">Star name</label>
          <input
            id="employee-star-name"
            name="star_name"
            required
            value={star.starName}
            onChange={(event) => updateStar('starName', event.target.value)}
          />

          <label htmlFor="employee-star-birth-year">Birth year (optional)</label>
          <input
            id="employee-star-birth-year"
            name="birth_year"
            type="number"
            min="0"
            value={star.birthYear}
            onChange={(event) => updateStar('birthYear', event.target.value)}
          />

          <button type="submit" disabled={starSubmitting}>
            {starSubmitting ? 'Inserting star...' : 'Insert star'}
          </button>
        </form>
        <ResultMessage result={starResult} />
      </section>

      <section className="employee-dashboard__section" aria-label="Add a movie">
        <h2>Add a movie</h2>
        <form className="stack-form" onSubmit={handleAddMovie}>
          <label htmlFor="employee-movie-title">Movie title</label>
          <input
            id="employee-movie-title"
            name="movie_title"
            required
            value={movie.movieTitle}
            onChange={(event) => updateMovie('movieTitle', event.target.value)}
          />

          <label htmlFor="employee-movie-year">Movie year</label>
          <input
            id="employee-movie-year"
            name="movie_year"
            type="number"
            min="0"
            required
            value={movie.movieYear}
            onChange={(event) => updateMovie('movieYear', event.target.value)}
          />

          <label htmlFor="employee-movie-director">Movie director</label>
          <input
            id="employee-movie-director"
            name="movie_director"
            required
            value={movie.movieDirector}
            onChange={(event) => updateMovie('movieDirector', event.target.value)}
          />

          <label htmlFor="employee-movie-star-name">Movie star name</label>
          <input
            id="employee-movie-star-name"
            name="movie_star_name"
            required
            value={movie.movieStarName}
            onChange={(event) => updateMovie('movieStarName', event.target.value)}
          />

          <label htmlFor="employee-movie-star-birth-year">Movie star birth year (optional)</label>
          <input
            id="employee-movie-star-birth-year"
            name="movie_star_birth_year"
            type="number"
            min="0"
            value={movie.movieStarBirthYear}
            onChange={(event) => updateMovie('movieStarBirthYear', event.target.value)}
          />

          <label htmlFor="employee-movie-genre-name">Movie genre name</label>
          <input
            id="employee-movie-genre-name"
            name="movie_genre_name"
            required
            value={movie.movieGenreName}
            onChange={(event) => updateMovie('movieGenreName', event.target.value)}
          />

          <button type="submit" disabled={movieSubmitting}>
            {movieSubmitting ? 'Adding movie...' : 'Add movie'}
          </button>
        </form>
        <ResultMessage result={movieResult} />
      </section>

      <div className="logout-control">
        <button type="button" disabled={loggingOut} onClick={handleLogout}>
          {loggingOut ? 'Signing out...' : 'Sign out'}
        </button>
        {logoutError && <span role="alert">{logoutError}</span>}
      </div>
    </section>
  )
}

export default EmployeeDashboardPage
