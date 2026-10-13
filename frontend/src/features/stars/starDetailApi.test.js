import { afterEach, describe, expect, it, vi } from 'vitest'
import { fetchStarDetail } from './starDetailApi.js'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('fetchStarDetail', () => {
  it('requests the single-star endpoint with the id', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ star_id: 'nm9000001', star_name: 'Alex Rivera' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetchMock)

    await fetchStarDetail('nm9000001')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/single-star?id=nm9000001',
      expect.anything(),
    )
  })

  it('surfaces not-found responses as API errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ errorMessage: 'Star not found.' }),
      { status: 404, headers: { 'Content-Type': 'application/json' } },
    )))

    await expect(fetchStarDetail('nm0000000')).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
      message: 'Star not found.',
    })
  })
})
