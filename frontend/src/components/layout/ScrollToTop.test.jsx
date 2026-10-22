import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Link, MemoryRouter, useLocation } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ScrollToTop from './ScrollToTop.jsx'

// jsdom has no scrollRestoration, so the component's guard would skip the
// property assignment it makes on a real browser. Defining it here lets the
// test observe that assignment.
function defineScrollRestoration(value) {
  Object.defineProperty(window.history, 'scrollRestoration', {
    value,
    writable: true,
    configurable: true,
  })
}

// MemoryRouter keeps its history in memory, so the current entry has to be
// read through the router rather than from window.location.
function LocationProbe() {
  const location = useLocation()
  return <p data-testid="location">{`${location.pathname}${location.search}`}</p>
}

function renderRoutes(initialPath = '/movies?genre=Drama') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <ScrollToTop />
      <Link to="/home">Home</Link>
      <Link to="/movies?genre=Comedy">Comedy</Link>
      <LocationProbe />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  defineScrollRestoration('auto')
})

afterEach(() => {
  delete window.history.scrollRestoration
  vi.restoreAllMocks()
})

describe('ScrollToTop', () => {
  it('takes over the browser scroll restore from history', () => {
    vi.spyOn(window, 'scrollTo').mockImplementation(() => {})

    renderRoutes()

    expect(window.history.scrollRestoration).toBe('manual')
  })

  it('still resets the offset where history has no scroll restore', () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    delete window.history.scrollRestoration

    renderRoutes()

    expect('scrollRestoration' in window.history).toBe(false)
    expect(scrollTo).toHaveBeenCalledWith(0, 0)
  })

  it('resets the offset on the initial render', () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})

    renderRoutes()

    expect(scrollTo).toHaveBeenCalledWith(0, 0)
  })

  it('resets the offset when the pathname changes', async () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    const user = userEvent.setup()
    renderRoutes()
    expect(scrollTo).toHaveBeenCalledTimes(1)

    await user.click(screen.getByRole('link', { name: 'Home' }))

    expect(screen.getByTestId('location')).toHaveTextContent('/home')
    expect(scrollTo).toHaveBeenCalledTimes(2)
    expect(scrollTo).toHaveBeenLastCalledWith(0, 0)
  })

  it('leaves a query change within the same pathname to the page', async () => {
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    const user = userEvent.setup()
    renderRoutes()
    expect(scrollTo).toHaveBeenCalledTimes(1)

    await user.click(screen.getByRole('link', { name: 'Comedy' }))

    expect(screen.getByTestId('location')).toHaveTextContent('/movies?genre=Comedy')
    expect(scrollTo).toHaveBeenCalledTimes(1)
  })
})
