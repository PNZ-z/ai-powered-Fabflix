import { afterEach, describe, expect, it } from 'vitest'
import { lastMovieListUrl, readLastMovieListQuery, saveLastMovieListQuery } from './lastMovieList.js'

afterEach(() => {
  window.sessionStorage.clear()
})

describe('lastMovieList storage', () => {
  it('round-trips the last movie list query', () => {
    saveLastMovieListQuery('genre=Drama&pageNumber=2&pageSize=25')

    expect(readLastMovieListQuery()).toBe('genre=Drama&pageNumber=2&pageSize=25')
    expect(lastMovieListUrl()).toBe('/movies?genre=Drama&pageNumber=2&pageSize=25')
  })

  it('falls back to the plain movie list route without stored state', () => {
    expect(readLastMovieListQuery()).toBe('')
    expect(lastMovieListUrl()).toBe('/movies')
  })

  it('ignores unavailable storage', () => {
    const original = window.sessionStorage
    delete window.sessionStorage

    expect(() => saveLastMovieListQuery('genre=Drama')).not.toThrow()
    expect(readLastMovieListQuery()).toBe('')
    expect(lastMovieListUrl()).toBe('/movies')

    window.sessionStorage = original
  })
})
