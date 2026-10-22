# Fabflix React frontend

This directory contains the React + Vite frontend introduced in Phase 2. It provides the client-side routing and shared application shell for the whole application; the legacy HTML/JavaScript frontend has been retired. The Issue #24 learning page remains available at `/playground`.

## Requirements

- Node.js 22.12 or newer (`engines` in `package.json`)
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
npm run test:coverage
npm run build
```

`npm run test:coverage` runs the unit/component tests through `@vitest/coverage-v8` and fails when line coverage drops below the 90% `lines` threshold in `vitest.config.js`.

Run the browser smoke tests (Playwright, chromium only, API mocked):

```shell
npx playwright install chromium
npm run test:e2e
```

`npm run test:e2e` builds the app, serves it with `vite preview` on `http://127.0.0.1:4173`, and runs `e2e/*.spec.js` at 360px and 1440px. The specs mock every `/api` call with `page.route`, so no backend or MySQL is required.

Preview the production build:

```shell
npm run preview
```

The production assets are written to `dist/` and are not committed. Spring Boot serves them when the executable jar is packaged with the Maven `frontend` profile:

```shell
cd frontend && npm ci && npm run build
cd .. && mvn -Pfrontend clean package   # fails if frontend/dist/index.html is missing
```

`mvn -Pfrontend` is an ordinary Maven build, so it requires JDK 21 with `JAVA_HOME` pointing at that installation; see the Java toolchain section in the root [`README.md`](../README.md#java-toolchain).

The default `mvn package`/`mvn verify` build does not need Node or `dist/`; the `frontend` profile copies `dist/` into `target/classes/static` and adds the dist-missing guard.

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

Set `VITE_RECAPTCHA_SITE_KEY` for deployments that use Google reCAPTCHA. Leave it empty with the Spring Boot `local` profile, whose local verifier intentionally accepts login requests without a token.
