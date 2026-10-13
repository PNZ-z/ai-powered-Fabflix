import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiRequest } from '../../api/apiClient.js'
import { fullTextSearchUrl } from './searchRoutes.js'

const debounceMilliseconds = 300

function AutocompleteSearch() {
  const navigate = useNavigate()
  const cacheRef = useRef(new Map())
  const [query, setQuery] = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [activeIndex, setActiveIndex] = useState(-1)
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const trimmedQuery = query.trim()

  useEffect(() => {
    if (trimmedQuery.length < 3 || cacheRef.current.has(trimmedQuery)) return undefined

    const controller = new AbortController()
    const timeout = window.setTimeout(async () => {
      setStatus('loading')
      setError('')

      try {
        const params = new URLSearchParams({ query: trimmedQuery })
        const results = await apiRequest(`/boot/autocomplete?${params}`, {
          signal: controller.signal,
        })
        cacheRef.current.set(trimmedQuery, results)
        setSuggestions(results)
        setActiveIndex(-1)
        setStatus('success')
      } catch (requestError) {
        if (requestError.cause?.name !== 'AbortError') {
          setSuggestions([])
          setError(requestError.message)
          setStatus('error')
        }
      }
    }, debounceMilliseconds)

    return () => {
      window.clearTimeout(timeout)
      controller.abort()
    }
  }, [trimmedQuery])

  function submitFullTextSearch() {
    if (trimmedQuery) navigate(fullTextSearchUrl(trimmedQuery))
  }

  function selectSuggestion(suggestion) {
    navigate(`/movies/${suggestion.data.movie_id}`)
  }

  function handleQueryChange(event) {
    const nextQuery = event.target.value
    const nextTrimmedQuery = nextQuery.trim()
    setQuery(nextQuery)
    setActiveIndex(-1)
    setError('')

    if (nextTrimmedQuery.length < 3) {
      setSuggestions([])
      setStatus('idle')
    } else if (cacheRef.current.has(nextTrimmedQuery)) {
      setSuggestions(cacheRef.current.get(nextTrimmedQuery))
      setStatus('success')
    } else {
      setSuggestions([])
      setStatus('idle')
    }
  }

  function handleKeyDown(event) {
    if (event.key === 'ArrowDown' && suggestions.length) {
      event.preventDefault()
      setActiveIndex((index) => (index + 1) % suggestions.length)
    } else if (event.key === 'ArrowUp' && suggestions.length) {
      event.preventDefault()
      setActiveIndex((index) => (index <= 0 ? suggestions.length - 1 : index - 1))
    } else if (event.key === 'Enter') {
      event.preventDefault()
      if (activeIndex >= 0) selectSuggestion(suggestions[activeIndex])
      else submitFullTextSearch()
    } else if (event.key === 'Escape') {
      setSuggestions([])
      setActiveIndex(-1)
      setStatus('idle')
    }
  }

  const listboxOpen = trimmedQuery.length >= 3 && status !== 'idle'

  return (
    <form className="discovery-card" onSubmit={(event) => {
      event.preventDefault()
      submitFullTextSearch()
    }}>
      <h2>Search movie titles</h2>
      <label htmlFor="full-text-search">Movie title</label>
      <div className="autocomplete">
        <input
          id="full-text-search"
          type="search"
          autoComplete="off"
          role="combobox"
          aria-autocomplete="list"
          aria-controls="movie-suggestions"
          aria-expanded={listboxOpen}
          aria-activedescendant={activeIndex >= 0 ? `movie-suggestion-${activeIndex}` : undefined}
          placeholder="Type at least 3 characters"
          value={query}
          onChange={handleQueryChange}
          onKeyDown={handleKeyDown}
        />
        <button type="submit" disabled={!trimmedQuery}>Search</button>

        {listboxOpen && (
          <div className="autocomplete__menu">
            {status === 'loading' && <p role="status">Loading suggestions...</p>}
            {status === 'error' && <p role="alert">{error}</p>}
            {status === 'success' && suggestions.length === 0 && (
              <p>No title suggestions found.</p>
            )}
            {status === 'success' && suggestions.length > 0 && (
              <ul id="movie-suggestions" role="listbox">
                {suggestions.map((suggestion, index) => (
                  <li
                    id={`movie-suggestion-${index}`}
                    key={suggestion.data.movie_id}
                    role="option"
                    aria-selected={activeIndex === index}
                    className={activeIndex === index ? 'autocomplete__option--active' : undefined}
                    onMouseDown={(event) => {
                      event.preventDefault()
                      selectSuggestion(suggestion)
                    }}
                  >
                    {suggestion.value}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    </form>
  )
}

export default AutocompleteSearch
