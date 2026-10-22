import { expect, test } from '@playwright/test'
import { mockApi } from './support/api.js'

// The shared mock answers the catalog with a single movie, which is too short
// to scroll. These specs need a list taller than the viewport, so they serve a
// full page of generated summaries in the shape the endpoint returns (compare
// `movieSummary` in e2e/support/api.js).
function movieSummaries(count) {
  return Array.from({ length: count }, (_, index) => ({
    movie_id: `tt80${String(index).padStart(5, '0')}`,
    movie_title: `Scroll Movie ${index + 1}`,
    movie_year: 2000 + index,
    movie_director: 'Avery Stone',
    movie_genres: 'Action, Drama',
    movie_rating: 8.5,
    movie_stars: [{ star_id: 'nm9000001', star_name: 'Alex Rivera' }],
  }))
}

// 30 movies keep "Next" enabled (the endpoint returns one row beyond the page
// size) and make the page several viewports tall at both widths.
async function useLongMovieList(page) {
  await mockApi(page, { customer: true })
  // Registered after mockApi, so this handler answers the movie-list endpoint
  // first for the rest of the test; every other request keeps the shared mock.
  await page.route('**/api/boot/movie-list**', (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(movieSummaries(30)),
  }))
}

function scrollY(page) {
  return page.evaluate(() => window.scrollY)
}

// A list nobody has visited yet scrolls to its own top on the frame after its
// first render. Waiting for that frame keeps a scroll issued here from racing a
// reset that would undo it.
async function settleMovieList(page) {
  await page.evaluate(() => new Promise((resolve) => {
    requestAnimationFrame(() => requestAnimationFrame(resolve))
  }))
}

async function scrollToBottom(page) {
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  await expect.poll(() => scrollY(page)).toBeGreaterThan(100)
}

test('leaving the movie list from the bottom shows the top of the new page', async ({ page }) => {
  await useLongMovieList(page)
  await page.goto('/movies')
  await expect(page.getByRole('link', { name: 'Scroll Movie 1', exact: true })).toBeVisible()
  await settleMovieList(page)
  await scrollToBottom(page)

  // The header is not sticky, so Playwright's click would scroll the page to
  // the top to reach the link and the test would prove nothing. Dispatching the
  // click navigates through the same router handler while the page stays
  // scrolled to the bottom, which is the situation the reset has to fix.
  await page.getByRole('link', { name: 'Home', exact: true }).dispatchEvent('click')

  await expect(page.getByRole('heading', { name: 'Find your next Fabflix movie' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Find your next Fabflix movie' }))
    .toBeInViewport()
  await expect.poll(() => scrollY(page)).toBeLessThanOrEqual(2)
})

test('paging to the next result page opens it at the top', async ({ page }) => {
  await useLongMovieList(page)
  await page.goto('/movies')
  await expect(page.getByRole('link', { name: 'Scroll Movie 1', exact: true })).toBeVisible()
  await settleMovieList(page)
  await scrollToBottom(page)

  await page.getByRole('button', { name: 'Next' }).click()

  await expect(page.getByText('Page 2')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Scroll Movie 1', exact: true })).toBeVisible()
  await expect.poll(() => scrollY(page)).toBeLessThanOrEqual(2)
})

test('returning to the same movie list query restores the scroll position', async ({ page }) => {
  await useLongMovieList(page)
  await page.goto('/movies')
  // The last row sits near the bottom of the page at both widths, so it is the
  // one row that is guaranteed to need scrolling to reach.
  const movieLink = page.getByRole('link', { name: 'Scroll Movie 10', exact: true })
  await expect(movieLink).toBeVisible()
  await settleMovieList(page)

  // Scrolling the title into view first means the click below cannot move the
  // page, so the offset recorded here is the one the list has to restore.
  await movieLink.scrollIntoViewIfNeeded()
  const leftAt = await scrollY(page)
  expect(leftAt).toBeGreaterThan(100)

  await movieLink.click()
  await expect(page.getByRole('heading', { name: 'Neon Horizon' })).toBeVisible()

  await page.getByRole('link', { name: 'Back to movie list' }).click()
  await expect(page.getByRole('heading', { name: 'Movie catalog' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Scroll Movie 10', exact: true })).toBeVisible()

  await expect.poll(async () => Math.abs((await scrollY(page)) - leftAt)).toBeLessThanOrEqual(10)
})
