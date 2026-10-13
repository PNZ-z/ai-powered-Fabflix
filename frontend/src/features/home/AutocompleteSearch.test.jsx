import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AutocompleteSearch from './AutocompleteSearch.jsx'

function LocationProbe() {
  const location = useLocation()
  return <p>Location: {location.pathname}{location.search}</p>
}

function renderSearch(fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  render(
    <MemoryRouter initialEntries={['/home']}>
      <Routes>
        <Route path="home" element={<AutocompleteSearch />} />
        <Route path="movies" element={<LocationProbe />} />
        <Route path="movies/:movieId" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('AutocompleteSearch', () => {
  it('submits full-text search parameters', async () => {
    renderSearch(vi.fn())
    const user = userEvent.setup()

    await user.type(screen.getByRole('combobox'), 'Shooting')
    await user.click(screen.getByRole('button', { name: 'Search' }))

    expect(screen.getByText(
      'Location: /movies?query=Shooting&mode=fulltext&pageNumber=0&pageSize=10',
    )).toBeInTheDocument()
  })

  it('debounces suggestions and supports keyboard selection', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify([
        { value: 'The Shawshank Redemption', data: { movie_id: 'tt0111161' } },
      ]),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    ))
    renderSearch(fetchMock)
    const user = userEvent.setup()

    await user.type(screen.getByRole('combobox'), 'sha')
    expect(fetchMock).not.toHaveBeenCalled()

    expect(await screen.findByRole('option', { name: 'The Shawshank Redemption' }))
      .toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/autocomplete?query=sha',
      expect.objectContaining({ credentials: 'include' }),
    )

    await user.keyboard('{ArrowDown}{Enter}')
    expect(screen.getByText('Location: /movies/tt0111161')).toBeInTheDocument()
  })

  it('shows empty and error suggestion states', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(
        JSON.stringify([]),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ))
      .mockRejectedValueOnce(new TypeError('offline'))
    renderSearch(fetchMock)
    const user = userEvent.setup()

    await user.type(screen.getByRole('combobox'), 'none')
    expect(await screen.findByText('No title suggestions found.')).toBeInTheDocument()

    await user.clear(screen.getByRole('combobox'))
    await user.type(screen.getByRole('combobox'), 'error')
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
  })
})
