import { expect, test } from '@playwright/test'
import { expectNoHorizontalOverflow, mockApi } from './support/api.js'

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
