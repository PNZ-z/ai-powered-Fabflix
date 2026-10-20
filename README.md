# AI-powered Fabflix

Fabflix is a Java movie-commerce application being modernized into a Spring Boot application, with Spring AI and agent workflows on the roadmap. The online application uses Spring Boot, JDBC, and MySQL and serves a React + Vite single-page frontend that Maven packages into the executable jar. The legacy browser-based HTML/JavaScript frontend has been retired — React is the only supported frontend. Offline import and maintenance programs remain available as standalone utilities.

## Modernization roadmap

Items 3 and 4 are planned work: no AI capability exists in the codebase yet.

1. **Foundation:** preserve behavior with characterization tests, enforce coverage for the deployed web application, remove committed secrets, and make local infrastructure reproducible.
2. **Spring Boot:** migrate one vertical slice at a time into controller, application-service, and repository layers while keeping API behavior stable.
3. **Spring AI (planned):** add evaluated AI features on top of the existing lexical search, separating what the current data supports from what needs new corpora:
   - **Natural-language search:** natural language in, structured query and ranking out, with an evaluated quality gate.
   - **Movie catalog semantic retrieval (doable now):** embed structured catalog cards — `title`, `year`, `director`, `genres`, `stars`, `rating` — so catalog questions such as "movies similar to X" or "1990s sci-fi" work without any external corpus.
   - **Project/developer knowledge RAG Q&A:** the first-version corpus is the existing `docs/` (11 markdown documents, excluding these AI roadmap drafts) plus this `README.md`; every answer must cite its sources.
   - **Movie plot/synopsis Q&A is out of scope for this phase:** the `movies` table has no synopsis field, so plot questions require an external synopsis corpus (data acquisition and licensing), deferred to a later stage.
   - Recommendations and multi-turn conversational features are deferred to the agent-platform stage.
4. **Agent workflows:** expose narrowly scoped tools with authorization, human approval for writes, budgets, audit logs, and end-to-end observability.

## Current architecture

```text
Browser (React + Vite single-page app)
          |
Spring MVC controllers and authentication filters
          |
Services and JDBC repositories
          |
        MySQL
```

## Java toolchain

Building the backend — every `mvn` command, including `mvn spring-boot:run` — requires **JDK 21**, and `JAVA_HOME` must point at a JDK 21 installation: Maven runs on the JDK that `JAVA_HOME` selects, and a `requireJavaVersion [21,22)` rule in `pom.xml` fails the build at the `validate` phase for any other version. The error message names the requirement and points back here.

Check the toolchain before building:

```shell
mvn -version   # must report "Java version: 21.x"
export JAVA_HOME=/path/to/jdk-21   # the JDK 21 installation directory
```

IDE-embedded Maven uses the IDE's project JDK, so set that to 21 as well.

The compiler still targets Java 17 (`maven.compiler.release=17` in `pom.xml`), so the produced bytecode is class file major version 61 and the packaged jar runs on a Java 17+ runtime. **JDK 21 is the build toolchain, not the language level.** CI builds on Java 21 too, and the `Dockerfile` uses JDK 21 in both JDK stages (`maven:3.9-eclipse-temurin-21` for the build, `eclipse-temurin:21-jre` for the runtime), so local builds, CI, and the container image all use the same JDK.

The Compose commands below build and run inside those images, so they do not depend on the host JDK; the `mvn` commands run on the host and do.

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

### Full stack with Docker Compose

`docker compose up -d` builds the React app and the Spring Boot jar inside the `app` image, starts MySQL, and serves the SPA on port `8081`:

```shell
cp .env.example .env   # then replace every placeholder password
docker compose up -d
docker compose ps
```

Open `http://localhost:8081/`. The Compose `app` service activates the Spring `local` profile (which bypasses external reCAPTCHA) so the demo stack runs without a Google key. **Production deployments must set `RECAPTCHA_SECRET_KEY`** and, at frontend build time, `VITE_RECAPTCHA_SITE_KEY`: Vite inlines that value during `npm run build`, so setting it at runtime has no effect.

For a quick health check run `GET http://localhost:8081/api/boot/health`.

### Local development

Run MySQL with Compose, then the backend with the `local` profile:

```shell
docker compose up -d mysql
docker compose ps
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile bypasses external reCAPTCHA verification and must never be used in production. The application listens on `FABFLIX_BOOT_PORT` or `8081` by default. Browse the React app with the Vite dev server described under [React frontend development](#react-frontend-development), or run the packaged app on `http://localhost:8081/`.

### Production build

The React assets are not committed. Build them first, then package the executable jar with the `frontend` profile — the build fails if `frontend/dist/index.html` is missing:

```shell
cd frontend && npm ci && npm run build && cd ..
mvn -Pfrontend clean package
java -jar target/fablix.jar --spring.profiles.active=local
```

`mvn package`/`mvn verify` without the `frontend` profile still build the executable jar but leave the static assets out, so the default CI build does not need Node.

## React frontend development

The Phase 2 React application is isolated under `frontend/`. It runs independently from Spring Boot during development and forwards `/api` requests to the backend. It requires Node.js 22.12 or newer.

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
npm run test:coverage
npm run build
npm run test:e2e   # after `npx playwright install chromium`
```

React is the only supported frontend and its production build is served by Spring Boot (see [Production build](#production-build)). See [`frontend/README.md`](frontend/README.md) for the frontend structure and commands.

## API verification

[`fabflix.http`](fabflix.http) contains customer and employee request examples that can be run from IntelliJ IDEA's HTTP client. Run requests in order so the client retains the authenticated session cookie.

## Tests and coverage

Run the complete verification lifecycle with:

```shell
mvn verify
```

CI enforces a **90% line-coverage gate** for the Spring Boot online application. One-off XML parsers and their import model, importer worker/tasks, local account setup, password initialization programs, and index-maintenance utilities are deliberately excluded because they are not part of the deployed request path. Their exclusion is defined centrally in `pom.xml`; new online application classes are covered by default.

JaCoCo HTML reports are generated under `target/site/jacoco/`.

## Security note

The repository previously contained development database credentials and a reCAPTCHA secret. Those credentials must be considered compromised and rotated in their respective services; removing them from the current source does not invalidate copies in Git history.
