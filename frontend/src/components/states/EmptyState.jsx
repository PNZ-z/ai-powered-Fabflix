import PageState from './PageState.jsx'

function EmptyState({ title = 'Nothing here yet', message, children }) {
  return (
    <PageState eyebrow="Empty" title={title} message={message}>
      {children}
    </PageState>
  )
}

export default EmptyState
