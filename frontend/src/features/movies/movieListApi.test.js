import { afterEach, describe, expect, it, vi } from 'vitest'
import { buildMovieListParams, fetchMovieList, genreBrowseUrl, movieListEndpoint } from './movieListApi.js'

function paramsFrom(query) {
  return new URLSearchParams(query)
}

function buildParams(query) {
  return buildMovieListParams(paramsFrom(query))
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('movieListEndpoint', () => {
  it('selects the browse endpoint for filter URLs', () => {
    expect(movieListEndpoint(paramsFrom('genre=Drama&pageNumber=0&pageSize=10'))).toBe('/boot/movie-list')
  })

  it('selects the full-text endpoint in fulltext mode', () => {
    expect(movieListEndpoint(paramsFrom('mode=fulltext&query=neon'))).toBe('/boot/full-text-search')
  })
})

describe('buildMovieListParams', () => {
  it('forwards every browse filter with pagination and sorting', () => {
    const params = buildParams(
      'title=alien&year=1979&director=ridley&star=weaver&genre=Horror&startsWith=A'
      + '&pageNumber=3&pageSize=25&sort1=rating&dir1=DESC&sort2=title&dir2=ASC',
    )

    expect(params.toString()).toBe(
      'title=alien&year=1979&director=ridley&star=weaver&genre=Horror&startsWith=A'
      + '&sort1=rating&dir1=DESC&sort2=title&dir2=ASC&pageNumber=3&pageSize=25',
    )
  })

  it('forwards only the query in fulltext mode', () => {
    const params = buildParams('mode=fulltext&query=neon+horizon&pageNumber=1&pageSize=25')

    expect(params.toString()).toBe('query=neon+horizon&pageNumber=1&pageSize=25')
  })

  it('keeps the zero page number and drops empty values', () => {
    const params = buildParams('genre=Drama&title=&pageNumber=0')

    expect(params.toString()).toBe('genre=Drama&pageNumber=0')
  })

  it('drops invalid paging values so the backend applies defaults', () => {
    const params = buildParams('genre=Drama&pageNumber=-4&pageSize=seven')

    expect(params.toString()).toBe('genre=Drama')
  })

  it('builds an empty query when the URL has no state', () => {
    expect(buildParams('').toString()).toBe('')
  })
})

describe('fetchMovieList', () => {
  it('requests the movie list endpoint with the built query', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify([]),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetchMock)

    await fetchMovieList(paramsFrom('genre=Drama&pageNumber=2&pageSize=25'))

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/movie-list?genre=Drama&pageNumber=2&pageSize=25',
      expect.anything(),
    )
  })

  it('requests the full-text endpoint in fulltext mode', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify([]),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetchMock)

    await fetchMovieList(paramsFrom('mode=fulltext&query=neon&pageNumber=0&pageSize=10'))

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/full-text-search?query=neon&pageNumber=0&pageSize=10',
      expect.anything(),
    )
  })

  it('returns an empty list for non-array payloads', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ errorMessage: 'unexpected' }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    )))

    await expect(fetchMovieList(paramsFrom(''))).resolves.toEqual([])
  })
})

describe('genreBrowseUrl', () => {
  it('restarts the browse at the first page keeping the page size', () => {
    expect(genreBrowseUrl('Science Fiction', 25)).toBe(
      '/movies?genre=Science+Fiction&pageNumber=0&pageSize=25',
    )
  })

  it('defaults the page size to 10', () => {
    expect(genreBrowseUrl('Drama', 10)).toBe('/movies?genre=Drama&pageNumber=0&pageSize=10')
  })
})
