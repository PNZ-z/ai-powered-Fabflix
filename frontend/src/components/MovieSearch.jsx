function MovieSearch({ id, value, onChange }) {
    return (
        <>
            <label htmlFor={id}>Search movies:</label>
            <input
                id={id}
                type="text"
                value={value}
                onChange={(event) => onChange(event.target.value)}
                placeholder="Enter a movie title"
            />
        </>
    )
}

export default MovieSearch
