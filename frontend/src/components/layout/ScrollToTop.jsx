import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

// React Router renders a new page into the document without touching the
// scroll offset, so a navigation would otherwise inherit the offset of the
// page the visitor just left. Only a pathname change counts as a new page
// here: a query change inside one pathname belongs to that page, and the movie
// list restores or resets its own offset for a new query.
function ScrollToTop() {
  const { pathname } = useLocation()

  // The browser's own history restore would scroll on Back/Forward at a moment
  // this application does not control, fighting both the reset below and the
  // movie list's restore. Handing the decision to the application makes the
  // order deterministic.
  useEffect(() => {
    if ('scrollRestoration' in window.history) {
      window.history.scrollRestoration = 'manual'
    }
  }, [])

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return null
}

export default ScrollToTop
