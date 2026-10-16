import { expect, test } from '@playwright/test'
import { expectNoHorizontalOverflow, mockApi } from './support/api.js'

test('employee signs in, reads metadata, adds a star, and signs out', async ({ page }) => {
  await mockApi(page)

  await page.goto('/employee/login')
  await expect(page.getByRole('heading', { name: 'Employee login' })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await page.getByLabel('Username').fill('admin@example.com')
  await page.getByLabel('Password').fill('secret')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page.getByRole('heading', { name: 'Employee dashboard' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'movies' })).toBeVisible()
  await expect(page.getByText('varchar(100)')).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await page.getByLabel('Star name', { exact: true }).fill('Ada Lovelace')
  await page.getByRole('button', { name: 'Insert star' }).click()
  await expect(page.getByRole('status')).toContainText('Star inserted successfully')

  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page.getByRole('heading', { name: 'Employee login' })).toBeVisible()
})
