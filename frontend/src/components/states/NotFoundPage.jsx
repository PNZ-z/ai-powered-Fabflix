import { Link, useLocation } from 'react-router-dom'
import PageState from './PageState.jsx'

function NotFoundPage() {
  const location = useLocation()

  return (
    <PageState
      eyebrow="404"
      title="Page not found"
      message={`No Fabflix page matches ${location.pathname}.`}
    >
      <Link className="button-link" to="/home">Return home</Link>
    </PageState>
  )
}

export default NotFoundPage
