import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import AddToCartButton from '../cart/AddToCartButton.jsx'
import DetailLinks from '../../components/detail/DetailLinks.jsx'
import DetailMeta from '../../components/detail/DetailMeta.jsx'
import ErrorState from '../../components/states/ErrorState.jsx'
import LoadingState from '../../components/states/LoadingState.jsx'
import PageState from '../../components/states/PageState.jsx'
import { genreBrowseUrl } from './movieListApi.js'
import { lastMovieListUrl } from './lastMovieList.js'
import { fetchMovieDetail } from './movieDetailApi.js'

// The backend packs every genre of the movie into one comma-separated string;
// the legacy detail page links each one back to a fresh genre browse.
function parseGenreLinks(genres) {
  if (typeof genres !== 'string' || genres.trim() === '') return []
  return genres
    .split(',')
    .map((genre) => genre.trim())
    .filter((genre) => genre !== '')
    .map((genre) => ({
      key: genre,
      to: genreBrowseUrl(genre, 10),
      label: genre,
    }))
}

// The backend pads starless movies with a "N/A" placeholder entry; only real
// stars become links.
function parseStarLinks(stars) {
  if (!Array.isArray(stars)) return []
  return stars
    .filter((star) => star && star.star_id && star.star_name !== 'N/A')
    .map((star) => ({
      key: star.star_id,
      to: `/stars/${star.star_id}`,
      label: star.star_name,
    }))
}

function MovieDetailPage() {
  const { movieId } = useParams()
  const [movie, setMovie] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    let active = true

    fetchMovieDetail(movieId)
      .then((result) => {
        if (!active) return
        setMovie(result)
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
  }, [movieId, retryCount])

  function retry() {
    setStatus('loading')
    setError(null)
    setRetryCount((count) => count + 1)
  }

  if (status === 'loading') {
    return <LoadingState message="Loading the movie..." />
  }

  // Unknown or invalid ids answer 404 with a plain errorMessage; retrying
  // cannot fix them, so they render a dedicated not-found state.
  if (status === 'error' && error.status === 404) {
    return (
      <PageState
        eyebrow="404"
        title="Movie not found"
        message={`No Fabflix movie matches “${movieId}”.`}
      >
        <Link className="button-link" to={lastMovieListUrl()}>Back to movie list</Link>
      </PageState>
    )
  }

  if (status === 'error') {
    return <ErrorState message={error.message} onRetry={retry} />
  }

  return (
    <section className="detail-page">
      <p className="route-placeholder__eyebrow">Movie details</p>
      <h1>{movie.movie_title}</h1>

      <DetailMeta
        items={[
          { label: 'Year', value: movie.movie_year },
          { label: 'Director', value: movie.movie_director },
          { label: 'Rating', value: movie.movie_rating },
        ]}
      />

      <p className="detail-page__cart">
        <AddToCartButton movieId={movie.movie_id} movieTitle={movie.movie_title} />
      </p>

      <div className="detail-section">
        <h2>Genres</h2>
        <DetailLinks links={parseGenreLinks(movie.movie_genres)} />
      </div>

      <div className="detail-section">
        <h2>Stars</h2>
        <DetailLinks links={parseStarLinks(movie.movie_stars)} />
      </div>

      <p className="detail-page__back">
        <Link to={lastMovieListUrl()}>Back to movie list</Link>
      </p>
    </section>
  )
}

export default MovieDetailPage
