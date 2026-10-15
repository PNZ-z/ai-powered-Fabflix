import { describe, expect, it } from 'vitest'
import { formatCents, toCents } from './money.js'

describe('money helpers', () => {
  it('converts server prices to integer cents without float drift', () => {
    expect(toCents(7.5)).toBe(750)
    expect(toCents(3.1)).toBe(310)
    expect(toCents(0)).toBe(0)
    expect(toCents(19.99)).toBe(1999)
  })

  it('formats cents as a two-decimal dollar amount', () => {
    expect(formatCents(0)).toBe('$0.00')
    expect(formatCents(750)).toBe('$7.50')
    expect(formatCents(1810)).toBe('$18.10')
  })

  it('sums line totals in cents without drift', () => {
    const items = [
      { price: 7.5, quantity: 2 },
      { price: 3.1, quantity: 1 },
    ]

    const totalCents = items.reduce(
      (sum, item) => sum + toCents(item.price) * item.quantity,
      0,
    )

    expect(formatCents(totalCents)).toBe('$18.10')
  })
})
