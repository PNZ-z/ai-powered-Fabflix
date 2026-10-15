import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AppRoutes from './app/AppRoutes.jsx'
import { AuthContext } from './auth/authContext.js'
import { routeDefinitions } from './app/routeDefinitions.js'

const authenticatedSession = { status: 'authenticated', user: {}, error: null }
const anonymousSession = { status: 'anonymous', user: null, error: null }
const failedSession = {
  status: 'error',
  user: null,
  error: new Error('Unable to reach the Fabflix server.'),
}

function renderRoute(path, sessions = {}) {
  const auth = {
    customer: authenticatedSession,
    employee: authenticatedSession,
    refreshCustomer: () => {},
    refreshEmployee: () => {},
    ...sessions,
  }

  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider value={auth}>
        <AppRoutes />
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

const movieSummaries = [{
  movie_id: 'tt0123456',
  movie_title: 'Example Movie',
  movie_year: 2020,
  movie_director: 'Example Director',
  movie_genres: 'Drama',
  movie_rating: 8.0,
  movie_stars: [{ star_id: 'nm0000001', star_name: 'Example Star' }],
}]

const movieDetail = {
  movie_id: 'tt7654321',
  movie_title: 'Example Detail Movie',
  movie_year: 2019,
  movie_director: 'Detail Director',
  movie_genres: 'Action, Drama',
  movie_rating: '7.5',
  movie_stars: [{ star_id: 'nm0000001', star_name: 'Example Star' }],
}

const starDetail = {
  star_id: 'nm0000001',
  star_name: 'Example Star',
  star_birth_year: 1970,
  star_movies: [{ movie_id: 'tt7654321', movie_title: 'Example Detail Movie' }],
}

const cartItems = [{
  movie_id: 'tt0123456',
  movie_title: 'Example Movie',
  quantity: 2,
  price: 7.5,
}]

const confirmationOrder = {
  order_date: '2026-10-14',
  total_amount: 15,
  lines: [{
    movie_id: 'tt0123456',
    movie_title: 'Example Movie',
    quantity: 2,
    unit_price: 7.5,
    line_total: 15,
  }],
}

function responseBody(url) {
  if (url.includes('/single-movie')) return movieDetail
  if (url.includes('/single-star')) return starDetail
  if (url.includes('/movie-list') || url.includes('/full-text-search')) return movieSummaries
  if (url.includes('/boot/checkout/token')) return { status: 'success', token: 'checkout-token' }
  if (url.includes('/boot/confirmation')) return confirmationOrder
  if (url.includes('/boot/cart')) return cartItems
  return [{ name: 'Drama' }]
}

describe('application routing', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.stubGlobal('fetch', vi.fn((input) => {
      const url = typeof input === 'string' ? input : input.url
      return Promise.resolve(new Response(
        JSON.stringify(responseBody(url)),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ))
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it.each(routeDefinitions)('renders the $title placeholder at $samplePath', (route) => {
    renderRoute(route.samplePath)

    expect(screen.getByRole('heading', { name: route.title })).toBeInTheDocument()
  })

  it('uses client-side navigation from the shared shell', async () => {
    const user = userEvent.setup()
    renderRoute('/home')

    await user.click(screen.getByRole('link', { name: 'Movies' }))

    expect(screen.getByRole('heading', { name: 'Movie catalog' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Movies' })).toHaveAttribute('aria-current', 'page')
  })

  it('redirects the root route to the customer home page', async () => {
    renderRoute('/')

    expect(await screen.findByRole('heading', { name: 'Find your next Fabflix movie' })).toBeInTheDocument()
  })

  it('renders the movie detail page for movie ids', async () => {
    renderRoute('/movies/tt7654321')

    expect(await screen.findByRole('heading', { name: 'Example Detail Movie' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Example Star' })).toHaveAttribute(
      'href',
      '/stars/nm0000001',
    )
  })

  it('renders the star detail page for star ids', async () => {
    renderRoute('/stars/nm0000001')

    expect(await screen.findByRole('heading', { name: 'Example Star' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Example Detail Movie' })).toHaveAttribute(
      'href',
      '/movies/tt7654321',
    )
  })

  it('renders the real cart page for authenticated customers', async () => {
    renderRoute('/cart')

    expect(await screen.findByRole('heading', { name: 'Shopping cart' })).toBeInTheDocument()
    expect(screen.getByText('Example Movie')).toBeInTheDocument()
    expect(screen.getByText('Total: $15.00')).toBeInTheDocument()
  })

  it('renders the checkout form for authenticated customers', async () => {
    renderRoute('/checkout')

    expect(await screen.findByRole('heading', { name: 'Checkout' })).toBeInTheDocument()
    expect(screen.getByLabelText('Credit card number')).toBeRequired()
  })

  it('renders the confirmation page for authenticated customers', async () => {
    renderRoute('/confirmation')

    expect(await screen.findByRole('heading', { name: 'Order confirmed' })).toBeInTheDocument()
    expect(screen.getByText('Order date: 2026-10-14')).toBeInTheDocument()
    expect(screen.getByText('Total: $15.00')).toBeInTheDocument()
  })

  it.each(['/cart', '/checkout', '/confirmation'])(
    'redirects anonymous customers away from %s',
    async (path) => {
      renderRoute(path, { customer: anonymousSession })

      expect(await screen.findByRole('heading', { name: 'Sign in to Fabflix' })).toBeInTheDocument()
    },
  )

  it('redirects anonymous employees to employee login', async () => {
    renderRoute('/employee/dashboard', { employee: anonymousSession })

    expect(await screen.findByRole('heading', { name: 'Employee login' })).toBeInTheDocument()
  })

  it('shows a user-facing error when session validation fails', () => {
    renderRoute('/movies', { customer: failedSession })

    expect(screen.getByRole('alert')).toHaveTextContent('Unable to reach the Fabflix server.')
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })

  it('renders a recovery link for unknown routes', async () => {
    const user = userEvent.setup()
    renderRoute('/missing-page')

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByText('No Fabflix page matches /missing-page.')).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Return home' }))

    expect(screen.getByRole('heading', { name: 'Find your next Fabflix movie' })).toBeInTheDocument()
  })
})
