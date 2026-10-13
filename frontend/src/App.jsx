import { BrowserRouter } from 'react-router-dom'
import AppRoutes from './app/AppRoutes.jsx'
import AuthProvider from './auth/AuthProvider.jsx'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
