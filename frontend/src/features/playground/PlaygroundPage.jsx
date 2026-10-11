import { useState } from 'react'
import BackendStatus from '../../components/BackendStatus.jsx'
import MovieCard from '../../components/MovieCard.jsx'
import MovieSearch from '../../components/MovieSearch.jsx'
import SortSelect from '../../components/MovieSort.jsx'
import movies from '../../data/movie.js'

function PlaygroundPage() {
  const [showMovies, setShowMovies] = useState(true)
  const [searchValue, setSearchValue] = useState('')
  const [sortOption, setSortOption] = useState('default')
  const [currentPage, setCurrentPage] = useState(1)
  const pageSize = 2

  const displayedMovies = [...movies]
    .filter((movie) => movie.title.toLowerCase().includes(searchValue.toLowerCase()))
    .sort((firstMovie, secondMovie) => {
      switch (sortOption) {
        case 'title-asc':
          return firstMovie.title.localeCompare(secondMovie.title)
        case 'title-desc':
          return secondMovie.title.localeCompare(firstMovie.title)
        case 'year-asc':
          return firstMovie.year - secondMovie.year
        case 'year-desc':
          return secondMovie.year - firstMovie.year
        default:
          return 0
      }
    })

  const totalPages = Math.max(1, Math.ceil(displayedMovies.length / pageSize))
  const startIndex = (currentPage - 1) * pageSize
  const paginatedMovies = displayedMovies.slice(startIndex, startIndex + pageSize)

  function updateSearch(value) {
    setSearchValue(value)
    setCurrentPage(1)
  }

  function updateSort(value) {
    setSortOption(value)
    setCurrentPage(1)
  }

  return (
    <section className="playground">
      <p className="route-placeholder__eyebrow">Issue #24 learning page</p>
      <h1>React playground</h1>
      <BackendStatus />

      <div className="playground__controls">
        <MovieSearch id="movie-search" value={searchValue} onChange={updateSearch} />
        <SortSelect value={sortOption} onChange={updateSort} />
        <button type="button" onClick={() => setShowMovies((isShown) => !isShown)}>
          {showMovies ? 'Hide Popular Movies' : 'Show Popular Movies'}
        </button>
      </div>

      {showMovies && (
        <section>
          <h2>Popular Movies</h2>
          {displayedMovies.length === 0 ? (
            <p>No movies found</p>
          ) : (
            <>
              <div className="movie-grid">
                {paginatedMovies.map((movie) => <MovieCard key={movie.id} movie={movie} />)}
              </div>
              <div className="pagination" aria-label="Movie pages">
                <button
                  type="button"
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage((page) => page - 1)}
                >
                  Previous
                </button>
                <span>Page {currentPage} of {totalPages}</span>
                <button
                  type="button"
                  disabled={currentPage === totalPages}
                  onClick={() => setCurrentPage((page) => page + 1)}
                >
                  Next
                </button>
              </div>
            </>
          )}
        </section>
      )}
    </section>
  )
}

export default PlaygroundPage
