import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../auth/ProtectedRoute.jsx'
import AppShell from '../components/layout/AppShell.jsx'
import NotFoundPage from '../components/states/NotFoundPage.jsx'
import LoginPage from '../features/auth/LoginPage.jsx'
import CartPage from '../features/cart/CartPage.jsx'
import ConfirmationPage from '../features/checkout/ConfirmationPage.jsx'
import PaymentPage from '../features/checkout/PaymentPage.jsx'
import HomePage from '../features/home/HomePage.jsx'
import MovieDetailPage from '../features/movies/MovieDetailPage.jsx'
import MovieListPage from '../features/movies/MovieListPage.jsx'
import RoutePlaceholderPage from '../features/placeholders/RoutePlaceholderPage.jsx'
import StarDetailPage from '../features/stars/StarDetailPage.jsx'
import PlaygroundPage from '../features/playground/PlaygroundPage.jsx'
import {
  customerProtectedRoutes,
  customerPublicRoutes,
  employeeProtectedRoutes,
  employeePublicRoutes,
} from './routeDefinitions.js'

function placeholderRoute(route) {
  return (
    <Route
      key={route.path}
      path={route.path}
      element={<RoutePlaceholderPage route={route} />}
    />
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/home" replace />} />
        <Route path="login" element={<LoginPage />} />
        {customerPublicRoutes.map(placeholderRoute)}
        <Route element={<ProtectedRoute audience="customer" />}>
          <Route path="home" element={<HomePage />} />
          <Route path="movies" element={<MovieListPage />} />
          <Route path="movies/:movieId" element={<MovieDetailPage />} />
          <Route path="stars/:starId" element={<StarDetailPage />} />
          <Route path="cart" element={<CartPage />} />
          <Route path="checkout" element={<PaymentPage />} />
          <Route path="confirmation" element={<ConfirmationPage />} />
          {customerProtectedRoutes.map(placeholderRoute)}
        </Route>
        {employeePublicRoutes.map(placeholderRoute)}
        <Route element={<ProtectedRoute audience="employee" />}>
          {employeeProtectedRoutes.map(placeholderRoute)}
        </Route>
        <Route path="playground" element={<PlaygroundPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default AppRoutes
