const customerPublicRoutes = []

const customerProtectedRoutes = [
  {
    path: 'cart',
    samplePath: '/cart',
    title: 'Shopping cart',
    description: 'Cart management will be migrated in Issue #30.',
  },
  {
    path: 'checkout',
    samplePath: '/checkout',
    title: 'Checkout',
    description: 'Payment will be migrated in Issue #30.',
  },
  {
    path: 'confirmation',
    samplePath: '/confirmation',
    title: 'Order confirmation',
    description: 'The confirmation flow will be migrated in Issue #30.',
  },
]

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
