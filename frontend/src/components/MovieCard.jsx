function MovieCard({ movie }) {
    return (
        <article>
            <h3>{movie.title}</h3>
            <p>Director: {movie.director}</p>
            <p>Year: {movie.year}</p>
        </article>
    )
}

export default MovieCard