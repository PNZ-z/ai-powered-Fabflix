const customerPublicRoutes = []

// Cart, checkout, and confirmation now render real pages wired explicitly in
// AppRoutes.jsx, so they are no longer placeholder route definitions.
const customerProtectedRoutes = []

const employeePublicRoutes = [
  {
    path: 'employee/login',
    samplePath: '/employee/login',
    title: 'Employee login',
    description: 'Employee authentication will be migrated in Issue #31.',
  },
]

const employeeProtectedRoutes = [
  {
    path: 'employee/dashboard',
    samplePath: '/employee/dashboard',
    title: 'Employee dashboard',
    description: 'Metadata and movie-management tools will be migrated in Issue #31.',
  },
]

const customerRoutes = [...customerPublicRoutes, ...customerProtectedRoutes]
const employeeRoutes = [...employeePublicRoutes, ...employeeProtectedRoutes]
const routeDefinitions = [...customerRoutes, ...employeeRoutes]

export {
  customerProtectedRoutes,
  customerPublicRoutes,
  customerRoutes,
  employeeProtectedRoutes,
  employeePublicRoutes,
  employeeRoutes,
  routeDefinitions,
}
