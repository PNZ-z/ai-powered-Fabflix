import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import DetailLinks from '../../components/detail/DetailLinks.jsx'
import DetailMeta from '../../components/detail/DetailMeta.jsx'
import ErrorState from '../../components/states/ErrorState.jsx'
import LoadingState from '../../components/states/LoadingState.jsx'
import PageState from '../../components/states/PageState.jsx'
import { lastMovieListUrl } from '../movies/lastMovieList.js'
import { fetchStarDetail } from './starDetailApi.js'

// The filmography answers every movie as { movie_id, movie_title }; each one
// links to the movie detail page.
function parseMovieLinks(movies) {
  if (!Array.isArray(movies)) return []
  return movies
    .filter((movie) => movie && movie.movie_id)
    .map((movie) => ({
      key: movie.movie_id,
      to: `/movies/${movie.movie_id}`,
      label: movie.movie_title,
    }))
}

function StarDetailPage() {
  const { starId } = useParams()
  const [star, setStar] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState(null)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    let active = true

    fetchStarDetail(starId)
      .then((result) => {
        if (!active) return
        setStar(result)
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
  }, [starId, retryCount])

  function retry() {
    setStatus('loading')
    setError(null)
    setRetryCount((count) => count + 1)
  }

  if (status === 'loading') {
    return <LoadingState message="Loading the star..." />
  }

  // Unknown or invalid ids answer 404 with a plain errorMessage; retrying
  // cannot fix them, so they render a dedicated not-found state.
  if (status === 'error' && error.status === 404) {
    return (
      <PageState
        eyebrow="404"
        title="Star not found"
        message={`No Fabflix star matches “${starId}”.`}
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
      <p className="route-placeholder__eyebrow">Star details</p>
      <h1>{star.star_name}</h1>

      <DetailMeta
        items={[
          { label: 'Birth year', value: star.star_birth_year ?? 'Unknown' },
        ]}
      />

      <div className="detail-section">
        <h2>Filmography</h2>
        <DetailLinks links={parseMovieLinks(star.star_movies)} />
      </div>

      <p className="detail-page__back">
        <Link to={lastMovieListUrl()}>Back to movie list</Link>
      </p>
    </section>
  )
}

export default StarDetailPage
