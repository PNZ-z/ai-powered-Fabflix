import { apiRequest } from '../../api/apiClient.js'

// Spring Boot endpoints that back the employee routes.
const employeeLoginEndpoint = '/boot/employee-login'
const dashboardEndpoint = '/boot/dashboard'
const employeeLogoutEndpoint = '/boot/dashboard/logout'

// AdminController matches each dashboard handler by the action query parameter
// (params = "action=..."), so every dashboard request must carry it.
const metadataAction = 'metadata'
const addStarAction = 'add-star'
const addMovieAction = 'add-movie'

function dashboardRequestPath(action) {
  return `${dashboardEndpoint}?${new URLSearchParams({ action }).toString()}`
}

function loginEmployee({ username, password }) {
  return apiRequest(employeeLoginEndpoint, {
    method: 'POST',
    body: new URLSearchParams({ username, password }),
  })
}

// Signing out only drops the employee identity; the matching customer session
// (if any) is left untouched by the backend.
function logoutEmployee() {
  return apiRequest(employeeLogoutEndpoint)
}

function fetchMetadata() {
  return apiRequest(dashboardRequestPath(metadataAction))
}

function addStar({ starName, birthYear }) {
  const body = new URLSearchParams({ star_name: starName, birth_year: birthYear })
  return apiRequest(dashboardRequestPath(addStarAction), { method: 'POST', body })
}

function addMovie({
  movieTitle,
  movieYear,
  movieDirector,
  movieStarName,
  movieStarBirthYear,
  movieGenreName,
}) {
  const body = new URLSearchParams({
    movie_title: movieTitle,
    movie_year: movieYear,
    movie_director: movieDirector,
    movie_star_name: movieStarName,
    movie_star_birth_year: movieStarBirthYear,
    movie_genre_name: movieGenreName,
  })
  return apiRequest(dashboardRequestPath(addMovieAction), { method: 'POST', body })
}

export {
  addMovie,
  addStar,
  dashboardRequestPath,
  fetchMetadata,
  loginEmployee,
  logoutEmployee,
}
