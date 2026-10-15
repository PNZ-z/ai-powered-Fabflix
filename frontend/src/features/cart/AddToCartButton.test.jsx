import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../auth/authContext.js'
import AddToCartButton from './AddToCartButton.jsx'
import CartStatusProvider from './CartStatusProvider.jsx'

// The button and its toast do not depend on a session, and an anonymous
// session keeps the provider from adding its own /boot/cart request to the
// calls a test asserts on.
const anonymousSession = { status: 'anonymous', user: null, error: null }

const neonHorizon = { movieId: 'tt1', movieTitle: 'Neon Horizon' }
const quietSignal = { movieId: 'tt2', movieTitle: 'Quiet Signal' }

function jsonResponse(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    { status, headers: { 'Content-Type': 'application/json' } },
  )
}

// Every POST /boot/cart answers 'success' with a fresh Response, so repeated
// adds stay readable. Tests that need a cart count use an authenticated session.
function cartFetchMock() {
  return vi.fn(() => Promise.resolve(jsonResponse({ status: 'success' })))
}

function renderButtons(fetchMock, movies = [neonHorizon]) {
  vi.stubGlobal('fetch', fetchMock)
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ customer: anonymousSession }}>
        <CartStatusProvider>
          {movies.map((movie) => (
            <AddToCartButton
              key={movie.movieId}
              movieId={movie.movieId}
              movieTitle={movie.movieTitle}
            />
          ))}
        </CartStatusProvider>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

function addButtons() {
  return screen.getAllByRole('button', { name: 'Add to cart' })
}

// waitFor/findBy cannot advance fake timers, so the toast is awaited by
// flushing the pending request microtasks instead. Timers still tick with real
// time, which keeps the awaited userEvent calls responsive.
async function settle() {
  await act(async () => {})
}

beforeEach(() => {
  window.localStorage.clear()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('AddToCartButton', () => {
  it('posts an add action and announces it with a toast', async () => {
    const fetchMock = cartFetchMock()
    renderButtons(fetchMock)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/boot/cart',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(String(fetchMock.mock.calls[0][1].body)).toBe('movieId=tt1&action=add')

    const toast = await screen.findByRole('status')
    expect(toast).toHaveTextContent('Added "Neon Horizon" to your cart.')
    // The success message moved out of the button's inline text.
    expect(toast.closest('.add-to-cart')).toBeNull()
    expect(screen.getByRole('button', { name: 'Add to cart' })).toBeEnabled()
    // An anonymous session reads no cart count, so the add is the only request.
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('hides the toast after two seconds', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const fetchMock = cartFetchMock()
    renderButtons(fetchMock)
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))
    await settle()

    expect(screen.getByRole('status')).toHaveTextContent('Added "Neon Horizon" to your cart.')

    await act(async () => {
      vi.advanceTimersByTime(2000)
    })

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('keeps one toast and restarts its countdown on the next add', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    renderButtons(cartFetchMock(), [neonHorizon, quietSignal])
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })

    await user.click(addButtons()[0])
    await settle()
    await act(async () => {
      vi.advanceTimersByTime(1500)
    })

    expect(screen.getAllByRole('status')).toHaveLength(1)

    await user.click(addButtons()[1])
    await settle()

    expect(screen.getAllByRole('status')).toHaveLength(1)
    expect(screen.getByRole('status')).toHaveTextContent('Added "Quiet Signal" to your cart.')

    // 1500ms after the second add the first toast would already be gone.
    await act(async () => {
      vi.advanceTimersByTime(1500)
    })
    expect(screen.getByRole('status')).toBeInTheDocument()

    await act(async () => {
      vi.advanceTimersByTime(500)
    })
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('closes the toast from its close button', async () => {
    const fetchMock = cartFetchMock()
    renderButtons(fetchMock)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))
    await user.click(await screen.findByRole('button', { name: 'Close notification' }))

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('stops showing the toast after the opt-out checkbox and still adds to the cart', async () => {
    const fetchMock = cartFetchMock()
    renderButtons(fetchMock, [neonHorizon, quietSignal])
    const user = userEvent.setup()

    await user.click(addButtons()[0])
    await user.click(await screen.findByRole('checkbox', { name: "Don't show again" }))

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(window.localStorage.getItem('fabflix.hideAddToCartToast')).toBe('true')

    await user.click(addButtons()[1])

    // Both adds still reached the server, and no toast was announced.
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    expect(fetchMock.mock.calls.map(([, options]) => options.method)).toEqual(['POST', 'POST'])
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('shows a safe inline message on failure without a toast', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ errorMessage: 'boom' }, 500))
    renderButtons(fetchMock)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Unable to add this movie to your cart.',
    )
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('asks the visitor to sign in on 401 without a toast', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ errorMessage: 'Authentication required.' }, 401),
    )
    renderButtons(fetchMock)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Please sign in to add movies to your cart.',
    )
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
