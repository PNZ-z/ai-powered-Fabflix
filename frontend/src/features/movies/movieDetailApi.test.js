import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchMovieDetail } from './movieDetailApi.js'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchMovieDetail', () => {
  it('requests the single-movie endpoint with the id', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ movie_id: 'tt9000001', movie_title: 'Neon Horizon' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetchMock)

    await fetchMovieDetail('tt9000001')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/single-movie?id=tt9000001',
      expect.anything(),
    )
  })

  it('surfaces not-found responses as API errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ errorMessage: 'Movie not found.' }),
      { status: 404, headers: { 'Content-Type': 'application/json' } },
    )))

    await expect(fetchMovieDetail('tt0000000')).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
      message: 'Movie not found.',
    })
  })
})
