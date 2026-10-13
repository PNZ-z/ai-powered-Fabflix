import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiRequest } from '../../api/apiClient.js'
import EmptyState from '../../components/states/EmptyState.jsx'
import ErrorState from '../../components/states/ErrorState.jsx'
import LoadingState from '../../components/states/LoadingState.jsx'
import AutocompleteSearch from './AutocompleteSearch.jsx'
import { movieListUrl } from './searchRoutes.js'

const titleInitials = [...'0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ']

function HomePage() {
  const navigate = useNavigate()
  const [genres, setGenres] = useState([])
  const [genreStatus, setGenreStatus] = useState('loading')
  const [genreError, setGenreError] = useState(null)
  const [genreRequest, setGenreRequest] = useState(0)

  useEffect(() => {
    let active = true

    apiRequest('/boot/main')
      .then((result) => {
        if (!active) return
        setGenres(result)
        setGenreStatus('success')
      })
      .catch((error) => {
        if (!active) return
        setGenreError(error)
        setGenreStatus('error')
      })

    return () => {
      active = false
    }
  }, [genreRequest])

  function retryGenres() {
    setGenreStatus('loading')
    setGenreError(null)
    setGenreRequest((value) => value + 1)
  }

  function handleAdvancedSearch(event) {
    event.preventDefault()
    const fields = new FormData(event.currentTarget)
    navigate(movieListUrl({
      title: fields.get('title') || '',
      year: fields.get('year') || '',
      director: fields.get('director') || '',
      star: fields.get('star') || '',
    }))
  }

  return (
    <section className="home-page">
      <header className="home-page__hero">
        <p className="route-placeholder__eyebrow">Movie discovery</p>
        <h1>Find your next Fabflix movie</h1>
        <p>Search by title, narrow the catalog, or browse by genre and first letter.</p>
      </header>

      <div className="discovery-grid">
        <AutocompleteSearch />

        <form className="discovery-card stack-form" onSubmit={handleAdvancedSearch}>
          <h2>Advanced search</h2>
          <label htmlFor="movie-title">Title</label>
          <input id="movie-title" name="title" />
          <label htmlFor="movie-year">Year</label>
          <input id="movie-year" name="year" type="number" min="1000" max="2100" />
          <label htmlFor="movie-director">Director</label>
          <input id="movie-director" name="director" />
          <label htmlFor="movie-star">Star</label>
          <input id="movie-star" name="star" />
          <button type="submit">Search catalog</button>
        </form>
      </div>

      <section className="browse-section">
        <h2>Browse by genre</h2>
        {genreStatus === 'loading' && <LoadingState message="Loading genres..." />}
        {genreStatus === 'error' && (
          <ErrorState message={genreError.message} onRetry={retryGenres} />
        )}
        {genreStatus === 'success' && genres.length === 0 && (
          <EmptyState title="No genres available" message="Try again later." />
        )}
        {genreStatus === 'success' && genres.length > 0 && (
          <div className="browse-links">
            {genres.map((genre) => (
              <Link key={genre.name} to={movieListUrl({ genre: genre.name })}>{genre.name}</Link>
            ))}
          </div>
        )}
      </section>

      <section className="browse-section">
        <h2>Browse by title</h2>
        <div className="browse-links browse-links--letters">
          {titleInitials.map((initial) => (
            <Link key={initial} to={movieListUrl({ startsWith: initial })}>{initial}</Link>
          ))}
        </div>
      </section>
    </section>
  )
}

export default HomePage
