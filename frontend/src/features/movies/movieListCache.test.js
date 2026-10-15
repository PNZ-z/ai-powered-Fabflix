import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  MOVIE_LIST_CACHE_TTL_MS,
  clearMovieListCache,
  clearMovieListEntry,
  loadMovieList,
  readMovieListEntry,
  readMovieListScroll,
  saveMovieListScroll,
} from './movieListCache.js'

const KEY = '/boot/movie-list?genre=Drama'
const OTHER_KEY = '/boot/movie-list?genre=Horror'
const MOVIES = [{ movie_id: 'tt9000001', movie_title: 'Neon Horizon' }]

function deferred() {
  let resolve
  const promise = new Promise((settle) => { resolve = settle })
  return { promise, resolve }
}

beforeEach(() => {
  clearMovieListCache()
})

afterEach(() => {
  clearMovieListCache()
  window.sessionStorage.clear()
  vi.restoreAllMocks()
})

describe('readMovieListEntry', () => {
  it('reports nothing for a key that was never cached', () => {
    expect(readMovieListEntry(KEY)).toBeNull()
  })

  it('keeps an entry fresh up to the TTL and stale afterwards', async () => {
    const cachedAt = 1_000_000
    const now = vi.spyOn(Date, 'now').mockReturnValue(cachedAt)
    await loadMovieList(KEY, () => Promise.resolve(MOVIES))

    expect(readMovieListEntry(KEY)).toEqual({ data: MOVIES, isFresh: true })

    now.mockReturnValue(cachedAt + MOVIE_LIST_CACHE_TTL_MS - 1)
    expect(readMovieListEntry(KEY).isFresh).toBe(true)

    now.mockReturnValue(cachedAt + MOVIE_LIST_CACHE_TTL_MS)
    expect(readMovieListEntry(KEY).isFresh).toBe(false)
  })
})

describe('loadMovieList', () => {
  it('shares one request between concurrent callers of the same key', async () => {
    const pending = deferred()
    const fetcher = vi.fn(() => pending.promise)

    const first = loadMovieList(KEY, fetcher)
    const second = loadMovieList(KEY, fetcher)

    expect(second).toBe(first)
    pending.resolve(MOVIES)

    await expect(first).resolves.toEqual(MOVIES)
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(readMovieListEntry(KEY)).toEqual({ data: MOVIES, isFresh: true })
  })

  it('requests again once the previous request settled', async () => {
    const fetcher = vi.fn(() => Promise.resolve(MOVIES))

    await loadMovieList(KEY, fetcher)
    await loadMovieList(KEY, fetcher)

    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('does not cache a failure and lets the next caller retry', async () => {
    const failure = new Error('offline')

    await expect(loadMovieList(KEY, () => Promise.reject(failure))).rejects.toBe(failure)
    expect(readMovieListEntry(KEY)).toBeNull()

    const fetcher = vi.fn(() => Promise.resolve(MOVIES))
    await expect(loadMovieList(KEY, fetcher)).resolves.toEqual(MOVIES)
    expect(fetcher).toHaveBeenCalledTimes(1)
  })

  it('reports a synchronous fetcher throw as a rejection without caching it', async () => {
    const failure = new Error('boom')

    await expect(loadMovieList(KEY, () => { throw failure })).rejects.toBe(failure)
    expect(readMovieListEntry(KEY)).toBeNull()
  })

  it('keeps the entries of different keys apart', async () => {
    await loadMovieList(KEY, () => Promise.resolve(MOVIES))
    await loadMovieList(OTHER_KEY, () => Promise.resolve([]))

    expect(readMovieListEntry(KEY).data).toEqual(MOVIES)
    expect(readMovieListEntry(OTHER_KEY).data).toEqual([])
  })
})

describe('cache clearing', () => {
  it('drops only the entry of the given key', async () => {
    await loadMovieList(KEY, () => Promise.resolve(MOVIES))
    await loadMovieList(OTHER_KEY, () => Promise.resolve(MOVIES))

    clearMovieListEntry(KEY)

    expect(readMovieListEntry(KEY)).toBeNull()
    expect(readMovieListEntry(OTHER_KEY)).not.toBeNull()
  })

  it('ignores a request that was cleared while it was still in flight', async () => {
    const pending = deferred()
    const request = loadMovieList(KEY, () => pending.promise)

    clearMovieListEntry(KEY)
    pending.resolve(MOVIES)

    await expect(request).resolves.toEqual(MOVIES)
    expect(readMovieListEntry(KEY)).toBeNull()
  })

  it('drops every entry at once', async () => {
    await loadMovieList(KEY, () => Promise.resolve(MOVIES))
    await loadMovieList(OTHER_KEY, () => Promise.resolve(MOVIES))

    clearMovieListCache()

    expect(readMovieListEntry(KEY)).toBeNull()
    expect(readMovieListEntry(OTHER_KEY)).toBeNull()
  })
})

describe('scroll offsets', () => {
  it('reads back the offset saved for a key', () => {
    saveMovieListScroll(KEY, 250)

    expect(readMovieListScroll(KEY)).toBe(250)
  })

  it('reports nothing for a key without a saved offset', () => {
    expect(readMovieListScroll(KEY)).toBeNull()
  })

  it('forgets the offset when the visitor left the list at the top', () => {
    saveMovieListScroll(KEY, 250)

    saveMovieListScroll(KEY, 0)

    expect(readMovieListScroll(KEY)).toBeNull()
  })

  it('stores offsets next to the jump-back query without touching it', () => {
    window.sessionStorage.setItem('fabflix.lastMovieQuery', 'genre=Drama')

    saveMovieListScroll(KEY, 250)

    expect(window.sessionStorage.getItem('fabflix.lastMovieQuery')).toBe('genre=Drama')
    expect(readMovieListScroll(KEY)).toBe(250)
  })
})
