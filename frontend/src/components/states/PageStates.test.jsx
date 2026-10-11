import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import EmptyState from './EmptyState.jsx'
import ErrorState from './ErrorState.jsx'
import LoadingState from './LoadingState.jsx'

describe('shared page states', () => {
  it('announces loading content', () => {
    render(<LoadingState message="Loading the catalog..." />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading the catalog...')
  })

  it('accepts empty-state content and actions', () => {
    render(
      <EmptyState title="Your cart is empty" message="Add a movie to continue.">
        <a href="/movies">Browse movies</a>
      </EmptyState>,
    )

    expect(screen.getByRole('heading', { name: 'Your cart is empty' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse movies' })).toBeInTheDocument()
  })

  it('exposes an optional retry action for errors', async () => {
    const retry = vi.fn()
    const user = userEvent.setup()
    render(<ErrorState message="The request failed." onRetry={retry} />)

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(screen.getByRole('alert')).toHaveTextContent('The request failed.')
    expect(retry).toHaveBeenCalledOnce()
  })
})
