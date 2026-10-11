# AI-powered Fabflix

Fabflix is a Java movie-commerce application being modernized into a Spring Boot and Spring AI agent platform. The online application uses Spring Boot, JDBC, and MySQL. A React + Vite frontend now lives alongside the existing browser-based JavaScript frontend so pages can be migrated incrementally. Legacy servlet endpoints have been retired; offline import and maintenance programs remain available as standalone utilities.

## Modernization roadmap

1. **Foundation:** preserve behavior with characterization tests, enforce coverage for the deployed web application, remove committed secrets, and make local infrastructure reproducible.
2. **Spring Boot:** migrate one vertical slice at a time into controller, application-service, and repository layers while keeping API behavior stable.
3. **Spring AI:** add evaluated natural-language search, recommendations, and retrieval-augmented movie Q&A.
4. **Agent workflows:** expose narrowly scoped tools with authorization, human approval for writes, budgets, audit logs, and end-to-end observability.

## Current architecture

```text
Browser (React + Vite during migration, legacy HTML + JavaScript retained)
          |
Spring MVC controllers and authentication filters
          |
Services and JDBC repositories
          |
        MySQL
```

## Local database

Requirements: Docker with Compose support.

1. Copy `.env.example` to `.env` and replace all placeholder passwords.
2. Start MySQL with `docker compose up -d mysql`.
3. Wait for `docker compose ps` to report the database as healthy.

The first startup creates the schema, stored procedure, and deterministic development data. The named Docker volume `fabflix-mysql-data` preserves the database across container restarts and `docker compose down`. To intentionally rebuild it from scratch, run `docker compose down -v` before starting MySQL again. This permanently removes the Compose-managed database volume.

Development-only accounts seeded by `database/dev-seed.sql`:

- Customer: `dev.customer@fabflix.local` / `dev-password`
- Employee: `dev.admin@fabflix.local` / `admin-password`

Never reuse these credentials outside local development.

## Runtime configuration

Secrets are supplied through environment variables and must not be committed:

| Variable | Purpose |
| --- | --- |
| `FABFLIX_DB_USER` | Database application user |
| `FABFLIX_DB_PASSWORD` | Database application password |
| `FABFLIX_DB_URL` | JDBC URL used by maintenance utilities |
| `RECAPTCHA_SECRET_KEY` | Server-side reCAPTCHA verification secret |

Spring Boot creates the application datasource from these variables. The default JDBC URL targets the Compose MySQL port at `localhost:3307`.

## Run the application

Start MySQL, wait for it to become healthy, and run the application with the local profile:

```shell
docker compose up -d mysql
docker compose ps
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile bypasses external reCAPTCHA verification and must never be used in production. The application listens on `FABFLIX_BOOT_PORT` or `8081` by default. Open:

```text
http://localhost:8081/login.html
```

For a quick health check:

```text
GET http://localhost:8081/api/boot/health
```

The executable application is built at `target/fablix.jar` by `mvn package` or `mvn verify` and can be run with `java -jar target/fablix.jar --spring.profiles.active=local`.

## React frontend development

The Phase 2 React application is isolated under `frontend/`. It runs independently from Spring Boot during development and forwards `/api` requests to the backend.

```shell
cd frontend
npm install
npm run dev
```

Copy `frontend/.env.example` to `frontend/.env` before the first run. By default, Vite serves React on `http://localhost:5173` and proxies API requests to Spring Boot on `http://localhost:8081`.

Run all current frontend checks with:

```shell
cd frontend
npm run lint
npm test
npm run build
```

The existing HTML/JavaScript frontend remains the supported application while React routes are migrated. See [`frontend/README.md`](frontend/README.md) for the frontend structure and commands.

## API verification

[`fabflix.http`](fabflix.http) contains customer and employee request examples that can be run from IntelliJ IDEA's HTTP client. Run requests in order so the client retains the authenticated session cookie.

## Tests and coverage

Run the complete verification lifecycle with:

```shell
mvn verify
```

CI enforces an **85% line-coverage gate** for the Spring Boot online application. One-off XML parsers and their import model, importer worker/tasks, local account setup, password initialization programs, and index-maintenance utilities are deliberately excluded because they are not part of the deployed request path. Their exclusion is defined centrally in `pom.xml`; new online application classes are covered by default.

JaCoCo HTML reports are generated under `target/site/jacoco/`.

## Security note

The repository previously contained development database credentials and a reCAPTCHA secret. Those credentials must be considered compromised and rotated in their respective services; removing them from the current source does not invalidate copies in Git history.
