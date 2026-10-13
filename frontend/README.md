# Fabflix React frontend

This directory contains the React + Vite frontend introduced in Phase 2. It provides the client-side routing and shared application shell used while pages are migrated incrementally. The Issue #24 learning page remains available at `/playground`, and the legacy HTML/JavaScript frontend remains available until feature parity is complete.

## Requirements

- Node.js supported by the current Vite version
- Spring Boot running on port `8081` for the health check

## Configuration

Copy `.env.example` to `.env` when setting up the project:

```shell
cp .env.example .env
```

On PowerShell:

```powershell
Copy-Item .env.example .env
```

`VITE_API_BASE_URL` is available to browser code and must never contain a secret. `FABFLIX_BACKEND_TARGET` is read by Vite to proxy local `/api` requests to Spring Boot.

## Commands

Install dependencies once:

```shell
npm install
```

Start the development server:

```shell
npm run dev
```

The React application runs on the URL printed by Vite, normally `http://localhost:5173`. During development, API requests are forwarded to Spring Boot at `http://localhost:8081`.

Run the quality checks:

```shell
npm run lint
npm test
npm run build
```

Preview the production build:

```shell
npm run preview
```

The production assets are written to `dist/`. Spring Boot integration and removal of the legacy frontend are intentionally deferred until the final Phase 2 issue.

## Source layout

```text
src/
├── app/              route definitions and the route tree
├── components/       shared layout, UI states, and small components
├── data/             temporary local learning data
├── features/         feature-owned pages and components
├── test/             shared test setup
├── App.jsx           browser-router boundary
└── main.jsx          React application entry point
```

## Route placeholders

Issue #25 reserves the stable URLs used by later migration work:

- Customer: `/login`, `/home`, `/movies`, `/movies/:movieId`, `/stars/:starId`, `/cart`, `/checkout`, and `/confirmation`
- Employee: `/employee/login` and `/employee/dashboard`
- Learning page: `/playground`

Vite is explicitly configured as a single-page application, so opening or refreshing one of these routes returns `index.html` and lets React Router choose the page.

## API and authentication

Issue #26 adds a shared API client under `src/api/`. It sends session cookies with every request, parses JSON responses, and turns HTTP or network failures into a consistent `ApiError`.

`AuthProvider` checks customer and employee sessions independently when the application starts. Customer pages redirect anonymous users to `/login`; employee pages redirect them to `/employee/login`. A temporary backend failure displays the shared retryable error state instead of incorrectly treating the user as logged out.
