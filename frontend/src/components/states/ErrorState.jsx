import PageState from './PageState.jsx'

function ErrorState({ title = 'Something went wrong', message, onRetry }) {
  return (
    <PageState eyebrow="Error" title={title} message={message} role="alert">
      {onRetry && <button type="button" onClick={onRetry}>Try again</button>}
    </PageState>
  )
}

export default ErrorState
