import { expect, test } from '@playwright/test'
import { expectNoHorizontalOverflow, mockApi } from './support/api.js'

test('customer signs in with the keyboard and reaches home', async ({ page }) => {
  await mockApi(page)

  await page.goto('/login')
  await expect(page.getByRole('heading', { name: 'Sign in to Fabflix' })).toBeVisible()
  await expectNoHorizontalOverflow(page)

  await page.getByLabel('Email').focus()
  await page.keyboard.type('viewer@example.com')
  await page.keyboard.press('Tab')
  await page.keyboard.type('secret')
  await page.keyboard.press('Enter')

  await expect(page.getByRole('heading', { name: 'Find your next Fabflix movie' })).toBeVisible()
  await expectNoHorizontalOverflow(page)
})

test('customer sees the failure message on rejected credentials', async ({ page }) => {
  await mockApi(page, { customerCredentialsValid: false })

  await page.goto('/login')
  await page.getByLabel('Email').fill('viewer@example.com')
  await page.getByLabel('Password').fill('wrong')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page.getByRole('alert')).toContainText('Invalid username or password.')
  await expect(page.getByRole('heading', { name: 'Sign in to Fabflix' })).toBeVisible()
})
