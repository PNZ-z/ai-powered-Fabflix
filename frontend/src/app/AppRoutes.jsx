import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../auth/ProtectedRoute.jsx'
import AppShell from '../components/layout/AppShell.jsx'
import NotFoundPage from '../components/states/NotFoundPage.jsx'
import LoginPage from '../features/auth/LoginPage.jsx'
import HomePage from '../features/home/HomePage.jsx'
import RoutePlaceholderPage from '../features/placeholders/RoutePlaceholderPage.jsx'
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
