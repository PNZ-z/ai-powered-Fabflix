import { Link } from 'react-router-dom'

// Comma-separated inline links shared by the movie and star detail pages,
// matching the legacy detail markup. Renders the fallback text when the
// record has nothing to link.
function DetailLinks({ links, fallback = 'N/A' }) {
  if (!Array.isArray(links) || links.length === 0) {
    return <p className="detail-links detail-links--empty">{fallback}</p>
  }

  return (
    <p className="detail-links">
      {links.map((link, index) => (
        <span key={link.key ?? link.to}>
          {index > 0 && ', '}
          <Link to={link.to}>{link.label}</Link>
        </span>
      ))}
    </p>
  )
}

export default DetailLinks
