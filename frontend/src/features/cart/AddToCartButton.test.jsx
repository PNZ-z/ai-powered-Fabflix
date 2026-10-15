import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import AddToCartButton from './AddToCartButton.jsx'

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

function renderButton(fetchMock) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <MemoryRouter>
      <AddToCartButton movieId="tt1" movieTitle="Neon Horizon" />
    </MemoryRouter>,
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('AddToCartButton', () => {
  it('posts an add action and shows an inline confirmation', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ status: 'success' }))
    renderButton(fetchMock)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/cart',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(String(fetchMock.mock.calls[0][1].body)).toBe('movieId=tt1&action=add')
    expect(await screen.findByRole('status')).toHaveTextContent('Added Neon Horizon to your cart.')
  })

  it('shows a safe inline message on failure', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ errorMessage: 'boom' }, 500))
    renderButton(fetchMock)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Unable to add this movie to your cart.',
    )
  })

  it('asks the visitor to sign in on 401', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ errorMessage: 'Authentication required.' }, 401),
    )
    renderButton(fetchMock)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Please sign in to add movies to your cart.',
    )
  })
})
