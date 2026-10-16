import { expect, test } from '@playwright/test'
import { expectNoHorizontalOverflow, mockApi } from './support/api.js'

test('customer adds a movie and completes checkout through to confirmation', async ({ page }) => {
  await mockApi(page, { customer: true, cart: [
    { movie_id: 'tt9000001', movie_title: 'Neon Horizon', quantity: 2, price: 7.5 },
  ] })

  await page.goto('/movies?genre=Drama')
  await expect(page.getByRole('link', { name: 'Neon Horizon' })).toBeVisible()

  await page.getByRole('link', { name: 'Neon Horizon' }).click()
  await expect(page.getByRole('heading', { name: 'Neon Horizon' })).toBeVisible()
  await page.getByRole('button', { name: 'Add to cart' }).click()

  await page.goto('/cart')
  await expect(page.getByRole('heading', { name: 'Shopping cart' })).toBeVisible()
  await expect(page.getByText('Total: $15.00')).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await page.getByRole('link', { name: 'Proceed to payment' }).click()
  await expect(page.getByRole('heading', { name: 'Checkout' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Place order' })).toBeEnabled()
  await expectNoHorizontalOverflow(page)

  await page.getByLabel('First name').fill('Ada')
  await page.getByLabel('Last name').fill('Lovelace')
  await page.getByLabel('Credit card number').fill('1234')
  await page.getByLabel('Expiration date').fill('2030-01-01')
  await page.getByRole('button', { name: 'Place order' }).click()

  await expect(page.getByRole('heading', { name: 'Order confirmed' })).toBeVisible()
  await expect(page.getByText('Neon Horizon')).toBeVisible()
  await expectNoHorizontalOverflow(page)
})
