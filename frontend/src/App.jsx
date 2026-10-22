import { BrowserRouter } from 'react-router-dom'
import AppRoutes from './app/AppRoutes.jsx'
import AuthProvider from './auth/AuthProvider.jsx'
import ScrollToTop from './components/layout/ScrollToTop.jsx'

function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
