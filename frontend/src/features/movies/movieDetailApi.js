import { apiRequest } from '../../api/apiClient.js'

// Fetch one movie through the legacy single-movie contract. The endpoint
// answers 404 with { errorMessage } for unknown ids and 400 when the id is
// blank, both surfaced by the API client as ApiError.
async function fetchMovieDetail(movieId) {
  const params = new URLSearchParams({ id: movieId })
  return apiRequest(`/boot/single-movie?${params}`)
}

export { fetchMovieDetail }
