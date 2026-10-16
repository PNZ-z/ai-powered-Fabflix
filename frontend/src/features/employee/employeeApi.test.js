import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  addMovie,
  addStar,
  dashboardRequestPath,
  fetchMetadata,
  loginEmployee,
  logoutEmployee,
} from './employeeApi.js'

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

function stubFetch(body = { status: 'success' }) {
  const fetchMock = vi.fn().mockResolvedValue(jsonResponse(body))
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('employeeApi', () => {
  // AdminController matches each dashboard handler by the action query
  // parameter, so a missing action would never reach the right endpoint.
  it('carries the action query the dashboard endpoints match on', () => {
    expect(dashboardRequestPath('metadata')).toBe('/boot/dashboard?action=metadata')
    expect(dashboardRequestPath('add-star')).toBe('/boot/dashboard?action=add-star')
    expect(dashboardRequestPath('add-movie')).toBe('/boot/dashboard?action=add-movie')
  })

  it('posts the credentials as a form-encoded employee login', async () => {
    const fetchMock = stubFetch({ status: 'success' })

    await loginEmployee({ username: 'admin@example.com', password: 'secret' })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/boot/employee-login')
    expect(options.method).toBe('POST')
    expect(options.body).toBeInstanceOf(URLSearchParams)
    expect(options.body.get('username')).toBe('admin@example.com')
    expect(options.body.get('password')).toBe('secret')
    expect(options.credentials).toBe('include')
  })

  it('signs out through the employee dashboard logout endpoint', async () => {
    const fetchMock = stubFetch({})

    await logoutEmployee()

    expect(fetchMock.mock.calls[0][0]).toBe('/api/boot/dashboard/logout')
  })

  it('requests database metadata with the metadata action', async () => {
    const fetchMock = stubFetch({ status: 'success', tables: [] })

    await fetchMetadata()

    expect(fetchMock.mock.calls[0][0]).toBe('/api/boot/dashboard?action=metadata')
  })

  it('posts add-star fields under their raw form names', async () => {
    const fetchMock = stubFetch({ status: 'success' })

    await addStar({ starName: 'Ada Lovelace', birthYear: '1815' })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/boot/dashboard?action=add-star')
    expect([...options.body.entries()]).toEqual([
      ['star_name', 'Ada Lovelace'],
      ['birth_year', '1815'],
    ])
  })

  it('posts add-movie fields under their raw form names', async () => {
    const fetchMock = stubFetch({ status: 'success' })

    await addMovie({
      movieTitle: 'Neon Horizon',
      movieYear: '2024',
      movieDirector: 'Ada',
      movieStarName: 'Grace',
      movieStarBirthYear: '1906',
      movieGenreName: 'Drama',
    })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/boot/dashboard?action=add-movie')
    expect([...options.body.entries()]).toEqual([
      ['movie_title', 'Neon Horizon'],
      ['movie_year', '2024'],
      ['movie_director', 'Ada'],
      ['movie_star_name', 'Grace'],
      ['movie_star_birth_year', '1906'],
      ['movie_genre_name', 'Drama'],
    ])
  })
})
