function PageState({ eyebrow, title, message, role, children }) {
  return (
    <section className="page-state" role={role} aria-live={role === 'status' ? 'polite' : undefined}>
      {eyebrow && <p className="page-state__eyebrow">{eyebrow}</p>}
      <h1>{title}</h1>
      {message && <p>{message}</p>}
      {children && <div className="page-state__actions">{children}</div>}
    </section>
  )
}

export default PageState
