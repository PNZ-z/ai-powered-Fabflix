// Client-side cache for the movie list with a stale-while-revalidate policy.
//
// Entries are keyed by the exact request path the backend receives (endpoint
// plus normalized query string), so two different URL states can never share
// one entry and URL parameters the endpoint never sees never split one. An
// entry younger than MOVIE_LIST_CACHE_TTL_MS is served without a request;
// an older entry is rendered immediately and revalidated in the background.
//
// The cache lives for the lifetime of the page: a browser reload starts empty,
// and a movie added by an administrator only shows up after the TTL expires
// (or after an explicit retry clears the entry). This module imports neither
// React nor the API layer — callers pass the cache key and the fetcher.
const MOVIE_LIST_CACHE_TTL_MS = 60_000

// Prefix for the remembered scroll offsets; the prefix keeps them apart from
// the unrelated `fabflix.lastMovieQuery` jump-back key in the same storage.
const SCROLL_STORAGE_PREFIX = 'fabflix.movieListScroll:'

const cachedEntries = new Map()
const inFlightRequests = new Map()

function readMovieListEntry(key) {
  const entry = cachedEntries.get(key)
  if (!entry) return null

  return {
    data: entry.data,
    isFresh: Date.now() - entry.cachedAt < MOVIE_LIST_CACHE_TTL_MS,
  }
}

// Loads the list for `key` through `fetcher`. Callers asking for the same key
// while a request is still running share that request, so a page that mounts
// twice (or React's development remount) hits the network once. Only a
// successful response is cached; a failure leaves an existing entry untouched
// and forgets the request so the next caller starts a fresh one.
function loadMovieList(key, fetcher) {
  const pending = inFlightRequests.get(key)
  if (pending) return pending

  const request = Promise.resolve()
    .then(fetcher)
    .then(
      (data) => {
        // clearMovieListEntry may have dropped this request while it was in
        // flight; such a response must not repopulate the cache.
        if (inFlightRequests.get(key) === request) {
          cachedEntries.set(key, { data, cachedAt: Date.now() })
          inFlightRequests.delete(key)
        }
        return data
      },
      (error) => {
        if (inFlightRequests.get(key) === request) {
          inFlightRequests.delete(key)
        }
        throw error
      },
    )

  inFlightRequests.set(key, request)
  return request
}

function clearMovieListEntry(key) {
  cachedEntries.delete(key)
  inFlightRequests.delete(key)
}

function clearMovieListCache() {
  cachedEntries.clear()
  inFlightRequests.clear()
}

// Scroll offsets live in sessionStorage instead of the maps above: they have
// to survive the reload a browser Back/Forward can trigger, while the cached
// movies deliberately do not. A missing or unreadable offset simply means the
// list opens at the top.
function readMovieListScroll(key) {
  try {
    const stored = window.sessionStorage.getItem(SCROLL_STORAGE_PREFIX + key)
    if (stored === null) return null

    const offset = Number.parseFloat(stored)
    return Number.isFinite(offset) && offset > 0 ? offset : null
  } catch {
    // Storage can be unavailable in private mode; restoring is optional.
    return null
  }
}

function saveMovieListScroll(key, offset) {
  try {
    const storageKey = SCROLL_STORAGE_PREFIX + key
    if (!Number.isFinite(offset) || offset <= 0) {
      // Leaving the list at the top must not restore a deeper offset later.
      window.sessionStorage.removeItem(storageKey)
      return
    }

    window.sessionStorage.setItem(storageKey, String(Math.round(offset)))
  } catch {
    // Storage can be unavailable in private mode; restoring is optional.
  }
}

export {
  MOVIE_LIST_CACHE_TTL_MS,
  clearMovieListCache,
  clearMovieListEntry,
  loadMovieList,
  readMovieListEntry,
  readMovieListScroll,
  saveMovieListScroll,
}
