import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// jsdom has no layout, so its window.scrollTo only reports "not implemented".
// Every page that resets or restores a scroll offset would print that error;
// the tests that assert on scrolling spy on this function themselves.
window.scrollTo = () => {}

afterEach(() => {
  cleanup()
})
