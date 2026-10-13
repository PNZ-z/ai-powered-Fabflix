import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import DetailLinks from './DetailLinks.jsx'
import DetailMeta from './DetailMeta.jsx'

function withRouter(element) {
  return render(<MemoryRouter>{element}</MemoryRouter>)
}

describe('DetailLinks', () => {
  it('renders comma-separated links', () => {
    withRouter(
      <DetailLinks
        links={[
          { key: 'nm1', to: '/stars/nm1', label: 'Alex Rivera' },
          { key: 'nm2', to: '/stars/nm2', label: 'Jamie Park' },
        ]}
      />,
    )

    expect(screen.getByRole('link', { name: 'Alex Rivera' })).toHaveAttribute('href', '/stars/nm1')
    expect(screen.getByRole('link', { name: 'Jamie Park' })).toHaveAttribute('href', '/stars/nm2')
    expect(screen.getByRole('link', { name: 'Alex Rivera' }).closest('p')).toHaveTextContent(
      'Alex Rivera, Jamie Park',
    )
  })

  it('renders the fallback text without links', () => {
    withRouter(<DetailLinks links={[]} fallback="No filmography yet" />)

    expect(screen.getByText('No filmography yet')).toBeInTheDocument()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })
})

describe('DetailMeta', () => {
  it('renders label and value rows', () => {
    withRouter(
      <DetailMeta
        items={[
          { label: 'Year', value: 2021 },
          { label: 'Rating', value: 'N/A' },
        ]}
      />,
    )

    expect(screen.getByText('Year')).toBeInTheDocument()
    expect(screen.getByText('2021')).toBeInTheDocument()
    expect(screen.getByText('Rating')).toBeInTheDocument()
    expect(screen.getByText('N/A')).toBeInTheDocument()
  })
})
