import { apiRequest } from '../../api/apiClient.js'

// Endpoints of the Spring Boot API that back the movie list route.
const browseEndpoint = '/boot/movie-list'
const fullTextEndpoint = '/boot/full-text-search'

// URL query parameters forwarded to the browse endpoint (legacy movieList.js contract).
const filterParamNames = ['title', 'year', 'director', 'star', 'genre', 'startsWith']
const sortParamNames = ['sort1', 'dir1', 'sort2', 'dir2']

function isFullTextSearch(searchParams) {
  return searchParams.get('mode') === 'fulltext'
}

function appendTrimmed(params, name, value) {
  if (typeof value !== 'string' || value.trim() === '') return
  params.set(name, value.trim())
}

function parseInteger(value) {
  if (typeof value !== 'string' || value.trim() === '') return null
  const parsed = Number.parseInt(value, 10)
  return Number.isInteger(parsed) ? parsed : null
}

function appendPageNumber(params, value) {
  const pageNumber = parseInteger(value)
  if (pageNumber !== null && pageNumber >= 0) {
    params.set('pageNumber', String(pageNumber))
  }
}

function appendPageSize(params, value) {
  const pageSize = parseInteger(value)
  if (pageSize !== null && pageSize > 0) {
    params.set('pageSize', String(pageSize))
  }
}

// Build the request query for the endpoint that backs the current URL state.
// Full-text mode forwards only the query; browse mode forwards every filter.
// Both modes forward the sort and pagination parameters. Invalid or negative
// paging values are dropped so the backend applies its defaults.
function buildMovieListParams(searchParams) {
  const params = new URLSearchParams()

  if (isFullTextSearch(searchParams)) {
    appendTrimmed(params, 'query', searchParams.get('query'))
  } else {
    filterParamNames.forEach((name) => {
      appendTrimmed(params, name, searchParams.get(name))
    })
  }

  sortParamNames.forEach((name) => {
    appendTrimmed(params, name, searchParams.get(name))
  })
  appendPageNumber(params, searchParams.get('pageNumber'))
  appendPageSize(params, searchParams.get('pageSize'))

  return params
}

function movieListEndpoint(searchParams) {
  return isFullTextSearch(searchParams) ? fullTextEndpoint : browseEndpoint
}

// The normalized request path identifies a list exactly: it carries the
// endpoint (browse vs full-text) plus the parameters the backend receives.
// The movie list cache uses it as its key, so URL state that changes the
// request never reuses another list and URL state the endpoint ignores never
// splits one.
function movieListRequestPath(searchParams) {
  const params = buildMovieListParams(searchParams)
  const suffix = params.toString() ? `?${params}` : ''
  return `${movieListEndpoint(searchParams)}${suffix}`
}

async function fetchMovieList(searchParams) {
  const results = await apiRequest(movieListRequestPath(searchParams))
  return Array.isArray(results) ? results : []
}

// Genre links from a movie row restart the browse at the first page while
// keeping the page size the visitor is currently using.
function genreBrowseUrl(genre, pageSize) {
  const params = new URLSearchParams({
    genre,
    pageNumber: '0',
    pageSize: String(pageSize),
  })
  return `/movies?${params}`
}

export {
  buildMovieListParams,
  fetchMovieList,
  genreBrowseUrl,
  movieListEndpoint,
  movieListRequestPath,
}
