import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '../components/layout/AppShell.jsx'
import NotFoundPage from '../components/states/NotFoundPage.jsx'
import RoutePlaceholderPage from '../features/placeholders/RoutePlaceholderPage.jsx'
import PlaygroundPage from '../features/playground/PlaygroundPage.jsx'
import { routeDefinitions } from './routeDefinitions.js'

function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/home" replace />} />
        {routeDefinitions.map((route) => (
          <Route
            key={route.path}
            path={route.path}
            element={<RoutePlaceholderPage route={route} />}
          />
        ))}
        <Route path="playground" element={<PlaygroundPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default AppRoutes
