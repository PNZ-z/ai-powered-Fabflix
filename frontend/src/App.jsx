import { useState } from 'react'
import MovieCard from './components/MovieCard.jsx'
import BackendStatus from './components/BackendStatus.jsx'
import MovieSearch from './components/MovieSearch.jsx'
import movies from './data/movie.js'
import SortSelect from './components/MovieSort.jsx'

function App() {
    const [showMovies, setShowMovies] = useState(true)
    const [searchValue, setSearchValue] = useState('')
    const [sortOption, setSortOption] = useState('default')
    const [currentPage, setCurrentPage] = useState(1)
    const pageSize = 2

    const filteredMovies = movies.filter((movie) =>
        movie.title.toLowerCase().includes(searchValue.toLowerCase()),
    )

    const displayedMovies = [...filteredMovies]

    switch (sortOption) {
        case 'title-asc':
            displayedMovies.sort((a, b) => a.title.localeCompare(b.title))
            break
        case 'title-desc':
            displayedMovies.sort((a, b) => b.title.localeCompare(a.title))
            break
        case 'year-asc':
            displayedMovies.sort((a, b) => a.year - b.year)
            break
        case 'year-desc':
            displayedMovies.sort((a, b) => b.year - a.year)
            break
        default:
            break
    }

    const totalPages = Math.max(1, Math.ceil(displayedMovies.length / pageSize))
    const startIndex = (currentPage - 1) * pageSize
    const paginatedMovies = displayedMovies.slice(startIndex, startIndex + pageSize)

    function toggleMovies() {
        setShowMovies((currentlyShown) => !currentlyShown)
    }

    function handleSearchChange(value) {
        setSearchValue(value)
        setCurrentPage(1)
    }

    function handleSortChange(value) {
        setSortOption(value)
        setCurrentPage(1)
    }


    return (
        <div>
            <h1>Fabflix</h1>
            <p>Welcome to Fabflix.</p>
            <BackendStatus/>

            <MovieSearch
                id="movie-search"
                value={searchValue}
                onChange={handleSearchChange}
            />
            <button onClick={toggleMovies}>
                {showMovies ? 'Hide Popular Movies' : 'Show Popular Movies'}
            </button>
            <SortSelect
                value={sortOption}
                onChange={handleSortChange}
            />

            {showMovies && (
                <>
                    <h2>Popular Movies</h2>

                    {displayedMovies.length === 0 ? (
                        <p>No movies found</p>
                    ) : (
                        <>
                            {paginatedMovies.map((movie) => (
                                <MovieCard key={movie.id} movie={movie}/>
                            ))}

                            <div>
                                <button
                                    disabled={currentPage === 1}
                                    onClick={() => setCurrentPage((page) => page - 1)}
                                >
                                    Previous
                                </button>

                                <span> Page {currentPage} of {totalPages} </span>

                                <button
                                    disabled={currentPage === totalPages}
                                    onClick={() => setCurrentPage((page) => page + 1)}
                                >
                                    Next
                                </button>
                            </div>
                        </>
                    )}
                </>
            )}
        </div>
    )
}

export default App
