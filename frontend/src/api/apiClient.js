const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

class ApiError extends Error {
  constructor(message, { status = 0, data = null, cause } = {}) {
    super(message, { cause })
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }

  get isUnauthorized() {
    return this.status === 401
  }
}

function buildUrl(path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}

async function parseResponse(response) {
  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    return response.json()
  }

  const text = await response.text()
  return text || null
}

function errorMessage(response, data) {
  if (data && typeof data === 'object') {
    return data.errorMessage || data.message || `Request failed with status ${response.status}`
  }

  return typeof data === 'string' && data.trim()
    ? data
    : `Request failed with status ${response.status}`
}

async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  let response
  try {
    response = await fetch(buildUrl(path), {
      ...options,
      credentials: 'include',
      headers,
    })
  } catch (cause) {
    throw new ApiError('Unable to reach the Fabflix server.', { cause })
  }

  let data
  try {
    data = await parseResponse(response)
  } catch (cause) {
    throw new ApiError('The Fabflix server returned an unreadable response.', {
      status: response.status,
      cause,
    })
  }
  if (!response.ok) {
    throw new ApiError(errorMessage(response, data), {
      status: response.status,
      data,
    })
  }

  return data
}

export { ApiError, apiRequest }
