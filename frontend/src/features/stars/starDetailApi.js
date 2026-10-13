import { apiRequest } from '../../api/apiClient.js'

// Fetch one star through the legacy single-star contract. The endpoint
// answers 404 with { errorMessage } for unknown ids and 400 when the id is
// blank, both surfaced by the API client as ApiError.
async function fetchStarDetail(starId) {
  const params = new URLSearchParams({ id: starId })
  return apiRequest(`/boot/single-star?${params}`)
}

export { fetchStarDetail }
