import { Link } from 'react-router-dom'
import { genreBrowseUrl } from './movieListApi.js'

// The backend packs the top three genres into one comma-separated string.
function parseGenres(genres) {
  if (typeof genres !== 'string' || genres.trim() === '') return []
  return genres
    .split(',')
    .map((genre) => genre.trim())
    .filter((genre) => genre !== '')
}

// The backend pads starless movies with a "N/A" placeholder row; the
// placeholder itself is never rendered as a link.
function parseStars(stars) {
  if (!Array.isArray(stars)) return []
  return stars.filter((star) => star && star.star_id && star.star_name !== 'N/A')
}

function MovieRow({ movie, pageSize }) {
  const genres = parseGenres(movie.movie_genres)
  const stars = parseStars(movie.movie_stars)

  return (
    <tr>
      <td>
        <Link to={`/movies/${movie.movie_id}`}>{movie.movie_title}</Link>
      </td>
      <td>{movie.movie_year}</td>
      <td>{movie.movie_director}</td>
      <td>
        {genres.length === 0 && 'N/A'}
        {genres.map((genre, index) => (
          <span key={genre}>
            {index > 0 && ', '}
            <Link to={genreBrowseUrl(genre, pageSize)}>{genre}</Link>
          </span>
        ))}
      </td>
      <td>
        {stars.length === 0 && 'N/A'}
        {stars.map((star, index) => (
          <span key={star.star_id}>
            {index > 0 && ', '}
            <Link to={`/stars/${star.star_id}`}>{star.star_name}</Link>
          </span>
        ))}
      </td>
      <td>{movie.movie_rating ?? 'N/A'}</td>
    </tr>
  )
}

function MovieTable({ movies, pageSize }) {
  return (
    <div className="movie-table-wrap">
      <table className="movie-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Year</th>
            <th>Director</th>
            <th>Genres</th>
            <th>Stars</th>
            <th>Rating</th>
          </tr>
        </thead>
        <tbody>
          {movies.map((movie) => (
            <MovieRow key={movie.movie_id} movie={movie} pageSize={pageSize} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default MovieTable
