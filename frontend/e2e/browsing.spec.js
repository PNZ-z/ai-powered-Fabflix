import { expect, test } from '@playwright/test'
import { expectNoHorizontalOverflow, mockApi } from './support/api.js'

// The state block is narrower than the catalog page, so it must still sit in
// the middle of the content area instead of hugging its left edge.
async function expectCentredInPageContent(page, selector) {
  const offsets = await page.evaluate((target) => {
    const stateBox = document.querySelector(target).getBoundingClientRect()
    const content = document.querySelector('.page-content')
    const contentBox = content.getBoundingClientRect()
    const contentStyle = getComputedStyle(content)

    return {
      stateCentre: stateBox.left + stateBox.width / 2,
      areaLeft: contentBox.left + Number.parseFloat(contentStyle.paddingLeft),
      areaRight: contentBox.right - Number.parseFloat(contentStyle.paddingRight),
    }
  }, selector)

  const areaCentre = (offsets.areaLeft + offsets.areaRight) / 2
  expect(Math.abs(offsets.stateCentre - areaCentre)).toBeLessThanOrEqual(2)
}

test('empty movie list centres the state block in the catalog page', async ({ page }) => {
  await mockApi(page, { customer: true })
  // Registered after mockApi, so it answers this endpoint first for the rest
  // of the test; the shared mock keeps serving every other request.
  await page.route('**/api/boot/movie-list**', (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: '[]',
  }))

  await page.goto('/movies')
  await expect(page.getByRole('heading', { name: 'No movies found' })).toBeVisible()
  await expectNoHorizontalOverflow(page)
  await expectCentredInPageContent(page, '.page-state')
})

test('authenticated customer browses from home to a movie detail', async ({ page }) => {
  await mockApi(page, { customer: true })

  await page.goto('/home')
  await expect(page.getByRole('heading', { name: 'Find your next Fabflix movie' })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await page.getByRole('link', { name: 'Drama' }).click()
  await expect(page.getByRole('heading', { name: 'Movie catalog' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Neon Horizon' })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await page.getByRole('link', { name: 'Neon Horizon' }).click()
  await expect(page.getByRole('heading', { name: 'Neon Horizon' })).toBeVisible()
  await expect(page.getByText('Avery Stone')).toBeVisible()
  // MovieDetail returns the rating as a string; the page must render it as-is.
  await expect(page.getByText('8.5')).toBeVisible()
  await expectNoHorizontalOverflow(page)
})

test('keyboard user can open the movie catalog from the main navigation', async ({ page }) => {
  await mockApi(page, { customer: true })

  await page.goto('/home')
  await expect(page.getByRole('heading', { name: 'Find your next Fabflix movie' })).toBeVisible()

  const moviesLink = page.getByRole('link', { name: 'Movies', exact: true })
  let focused = false
  for (let attempt = 0; attempt < 8 && !focused; attempt += 1) {
    await page.keyboard.press('Tab')
    focused = await moviesLink.evaluate((element) => element === document.activeElement)
  }
  expect(focused, 'Tab must reach the Movies navigation link').toBe(true)

  await page.keyboard.press('Enter')
  await expect(page.getByRole('heading', { name: 'Movie catalog' })).toBeVisible()
  await expectNoHorizontalOverflow(page)
})
