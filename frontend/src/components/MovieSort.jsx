function SortSelect({ value, onChange }) {
    return (
        <>
            <label htmlFor="movie-sort">Sort movies:</label>
            <select
                id="movie-sort"
                value={value}
                onChange={(event) => onChange(event.target.value)}
            >
                <option value="default">Default</option>
                <option value="title-asc">Title A-Z</option>
                <option value="title-desc">Title Z-A</option>
                <option value="year-desc">Newest first</option>
                <option value="year-asc">Oldest first</option>
            </select>
        </>
    )
}

export default SortSelect
