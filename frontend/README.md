# Fabflix React frontend

This directory contains the React + Vite frontend introduced in Phase 2. It currently provides a small learning page that proves the frontend toolchain, Spring Boot development proxy, component structure, state handling, and automated tests. The legacy HTML/JavaScript frontend remains available while routes are migrated incrementally.

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
├── components/       reusable React components and component tests
├── data/             temporary local learning data
├── test/             shared test setup
├── App.jsx           page state and derived movie-list data
└── main.jsx          React application entry point
```
