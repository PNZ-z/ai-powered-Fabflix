// The legacy detail pages jumped back to the movie list through the
// /api/boot/jump-back endpoint, which replays the query string stored in the
// server session. The React app keeps the same promise client-side so every
// browse or full-text list — not only full-text searches — can be restored.
const STORAGE_KEY = 'fabflix.lastMovieQuery'

function saveLastMovieListQuery(query) {
  try {
    window.sessionStorage.setItem(STORAGE_KEY, query)
  } catch {
    // Storage can be unavailable in private mode; the back link then falls
    // back to the plain movie list route.
  }
}

function readLastMovieListQuery() {
  try {
    return window.sessionStorage.getItem(STORAGE_KEY) || ''
  } catch {
    return ''
  }
}

function lastMovieListUrl() {
  const query = readLastMovieListQuery()
  return query ? `/movies?${query}` : '/movies'
}

export { lastMovieListUrl, readLastMovieListQuery, saveLastMovieListQuery }
