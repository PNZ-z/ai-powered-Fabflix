import PageState from './PageState.jsx'

function LoadingState({ message = 'Loading your movies...' }) {
  return <PageState eyebrow="Please wait" title="Loading" message={message} role="status" />
}

export default LoadingState
