import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest } from './apiClient.js'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('apiRequest', () => {
  it('sends session cookies and parses JSON responses', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ status: 'UP' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiRequest('/boot/health')).resolves.toEqual({ status: 'UP' })
    expect(fetchMock).toHaveBeenCalledWith('/api/boot/health', expect.objectContaining({
      credentials: 'include',
    }))
    expect(fetchMock.mock.calls[0][1].headers.get('Accept')).toBe('application/json')
  })

  it('normalizes unauthorized responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ errorMessage: 'Authentication required.' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } },
    )))

    const error = await apiRequest('/boot/main').catch((caught) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.message).toBe('Authentication required.')
    expect(error.status).toBe(401)
    expect(error.isUnauthorized).toBe(true)
  })

  it('uses the server message for other HTTP failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ message: 'Database unavailable.' }),
      { status: 503, headers: { 'Content-Type': 'application/json' } },
    )))

    await expect(apiRequest('/boot/main')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Database unavailable.',
      status: 503,
      isUnauthorized: false,
    })
  })

  it('normalizes network failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(apiRequest('/boot/health')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Unable to reach the Fabflix server.',
      status: 0,
    })
  })

  it('normalizes invalid JSON responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      'not-json',
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))

    await expect(apiRequest('/boot/health')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'The Fabflix server returned an unreadable response.',
      status: 200,
    })
  })
})
