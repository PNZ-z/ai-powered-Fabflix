import { expect } from '@playwright/test'

// Shared fixtures and a stateful API mock for the Playwright smoke tests. The
// mock listens on every /api request so the specs never touch a real backend.

const CUSTOMER_SESSION = {
  authenticated: true,
  role: 'customer',
  username: 'viewer@example.com',
  displayName: null,
}

const EMPLOYEE_SESSION = {
  authenticated: true,
  role: 'employee',
  username: 'admin@example.com',
  displayName: 'Admin User',
}

const movieSummary = {
  movie_id: 'tt9000001',
  movie_title: 'Neon Horizon',
  movie_year: 2021,
  movie_director: 'Avery Stone',
  movie_genres: 'Action, Drama',
  movie_rating: 8.5,
  movie_stars: [{ star_id: 'nm9000001', star_name: 'Alex Rivera' }],
}

const movieDetail = {
  ...movieSummary,
  // MovieDetail.getMovieRating() returns a String, unlike the list summary.
  movie_rating: '8.5',
  movie_stars: [{ star_id: 'nm9000001', star_name: 'Alex Rivera' }],
}

const cartItems = [
  { movie_id: 'tt9000001', movie_title: 'Neon Horizon', quantity: 2, price: 7.5 },
]

const placedOrder = {
  order_date: '2026-10-15',
  total_amount: 15,
  lines: [
    {
      movie_id: 'tt9000001',
      movie_title: 'Neon Horizon',
      quantity: 2,
      unit_price: 7.5,
      line_total: 15,
    },
  ],
}

const databaseMetadata = {
  status: 'success',
  tables: [
    {
      table_name: 'movies',
      columns: [
        { column_name: 'id', column_type: 'varchar(10)' },
        { column_name: 'title', column_type: 'varchar(100)' },
      ],
    },
  ],
}

function json(body, status = 200) {
  return { status, contentType: 'application/json', body: JSON.stringify(body) }
}

export async function mockApi(page, options = {}) {
  const state = {
    customer: options.customer ?? false,
    employee: options.employee ?? false,
    customerCredentialsValid: options.customerCredentialsValid ?? true,
    cart: options.cart ?? [],
  }

  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const { pathname, searchParams } = new URL(request.url())
    const method = request.method()

    if (pathname.endsWith('/boot/session/customer')) {
      await route.fulfill(state.customer ? json(CUSTOMER_SESSION) : json({}, 401))
      return
    }
    if (pathname.endsWith('/boot/dashboard/session')) {
      await route.fulfill(state.employee ? json(EMPLOYEE_SESSION) : json({}, 401))
      return
    }
    if (pathname.endsWith('/boot/dashboard/logout')) {
      state.employee = false
      await route.fulfill({ status: 204 })
      return
    }
    if (pathname.endsWith('/boot/logout')) {
      state.customer = false
      await route.fulfill({ status: 204 })
      return
    }
    if (pathname.endsWith('/boot/login') && method === 'POST') {
      if (state.customerCredentialsValid) {
        state.customer = true
        await route.fulfill(json({ status: 'success', message: 'Login successful.' }))
      } else {
        await route.fulfill(json({ status: 'fail', message: 'Invalid username or password.' }))
      }
      return
    }
    if (pathname.endsWith('/boot/employee-login') && method === 'POST') {
      state.employee = true
      await route.fulfill(json({ status: 'success', message: 'Employee login successful.' }))
      return
    }
    if (pathname.endsWith('/boot/main')) {
      await route.fulfill(json([{ name: 'Drama' }, { name: 'Comedy' }]))
      return
    }
    if (pathname.endsWith('/boot/movie-list') || pathname.endsWith('/boot/full-text-search')) {
      await route.fulfill(json([movieSummary]))
      return
    }
    if (pathname.endsWith('/boot/single-movie')) {
      await route.fulfill(json(movieDetail))
      return
    }
    if (pathname.endsWith('/boot/cart')) {
      await route.fulfill(method === 'POST' ? json({ status: 'success' }) : json(state.cart))
      return
    }
    if (pathname.endsWith('/boot/checkout/token')) {
      await route.fulfill(json({ status: 'success', token: 'e2e-checkout-token' }))
      return
    }
    if (pathname.endsWith('/boot/payment') && method === 'POST') {
      await route.fulfill(json({
        status: 'success',
        message: 'Order placed successfully.',
        order: placedOrder,
      }))
      return
    }
    if (pathname.endsWith('/boot/confirmation')) {
      // No order exists until a payment is placed; the API answers 204.
      await route.fulfill({ status: 204 })
      return
    }
    if (pathname.endsWith('/boot/dashboard') && searchParams.get('action') === 'metadata') {
      await route.fulfill(json(databaseMetadata))
      return
    }
    if (pathname.endsWith('/boot/dashboard') && searchParams.get('action') === 'add-star') {
      await route.fulfill(json({
        status: 'success',
        message: 'Star inserted successfully. New star ID: nm0000009',
      }))
      return
    }
    if (pathname.endsWith('/boot/dashboard') && searchParams.get('action') === 'add-movie') {
      await route.fulfill(json({ status: 'success', message: 'Movie added successfully.' }))
      return
    }

    await route.fulfill(json({ status: 'fail', message: `Unmocked ${method} ${pathname}` }, 404))
  })

  return state
}

// The responsive gate from the issue: a page must not scroll sideways at 360px
// (or 1440px). Allow a single pixel for sub-pixel rounding.
export async function expectNoHorizontalOverflow(page) {
  const { scrollWidth, innerWidth } = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    innerWidth: window.innerWidth,
  }))

  expect(scrollWidth).toBeLessThanOrEqual(innerWidth + 1)
}
