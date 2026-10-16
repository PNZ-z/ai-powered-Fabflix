import axe from 'axe-core'
import { expect } from 'vitest'

// axe-core runs inside jsdom, which has no layout engine and cannot compute
// rendered colors, so `color-contrast` is always disabled here and checked
// manually (see the issue #32 delivery report).
async function checkA11y(container, { includeRegion }) {
  const rules = {
    'color-contrast': { enabled: false },
    // Page components are rendered in isolation, without the AppShell <main>
    // landmark they always have in the running app; the shell tests keep the
    // rule enabled so landmarks are still checked.
    region: { enabled: includeRegion },
  }

  const results = await axe.run(container, { rules })

  const violations = results.violations.map(({ id, impact, nodes }) => ({
    id,
    impact,
    targets: nodes.map((node) => node.target),
  }))

  expect(violations).toEqual([])
}

export async function expectNoA11yViolations(container = document.body) {
  return checkA11y(container, { includeRegion: false })
}

// For trees that already contain their own landmarks (the app shell), so the
// `region` rule stays enabled.
export async function expectNoShellA11yViolations(container = document.body) {
  return checkA11y(container, { includeRegion: true })
}
