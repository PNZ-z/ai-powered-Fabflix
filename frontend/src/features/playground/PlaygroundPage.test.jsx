import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PlaygroundPage from './PlaygroundPage.jsx'

describe('PlaygroundPage', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ status: 'UP' }),
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('paginates the local movie list', async () => {
    const user = userEvent.setup()
    render(<PlaygroundPage />)

    expect(screen.getByText('The Shawshank Redemption')).toBeInTheDocument()
    expect(screen.getByText('The Godfather')).toBeInTheDocument()
    expect(screen.queryByText('The Dark Knight')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(screen.getByText('The Dark Knight')).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 2')).toBeInTheDocument()
  })

  it('filters movies and resets pagination', async () => {
    const user = userEvent.setup()
    render(<PlaygroundPage />)

    await user.click(screen.getByRole('button', { name: 'Next' }))
    await user.type(screen.getByRole('textbox', { name: 'Search movies:' }), 'god')

    expect(screen.getByText('The Godfather')).toBeInTheDocument()
    expect(screen.queryByText('The Dark Knight')).not.toBeInTheDocument()
    expect(screen.getByText('Page 1 of 1')).toBeInTheDocument()
  })

  it('sorts the displayed movies', async () => {
    const user = userEvent.setup()
    render(<PlaygroundPage />)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Sort movies:' }), 'year-desc')

    const titles = screen.getAllByRole('heading', { level: 3 })
      .map((heading) => heading.textContent)

    expect(titles).toEqual(['The Dark Knight', 'The Shawshank Redemption'])
  })
})
