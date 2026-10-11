import { useParams } from 'react-router-dom'

function RoutePlaceholderPage({ route }) {
  const params = useParams()
  const identifier = params.movieId || params.starId

  return (
    <section className="route-placeholder">
      <p className="route-placeholder__eyebrow">React migration route</p>
      <h1>{route.title}</h1>
      <p>{route.description}</p>
      {identifier && <p className="route-placeholder__id">Requested ID: {identifier}</p>}
    </section>
  )
}

export default RoutePlaceholderPage
