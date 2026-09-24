# AI-powered Fabflix

Fabflix is a legacy Java movie-commerce application being modernized incrementally into a Spring Boot and Spring AI agent platform. The current application uses Jakarta Servlet, JDBC, MySQL, and a browser-based JavaScript frontend.

## Modernization roadmap

1. **Foundation:** preserve behavior with characterization tests, enforce coverage for the deployed web application, remove committed secrets, and make local infrastructure reproducible.
2. **Spring Boot:** migrate one vertical slice at a time into controller, application-service, and repository layers while keeping API behavior stable.
3. **Spring AI:** add evaluated natural-language search, recommendations, and retrieval-augmented movie Q&A.
4. **Agent workflows:** expose narrowly scoped tools with authorization, human approval for writes, budgets, audit logs, and end-to-end observability.

## Current architecture

```text
Browser (HTML + JavaScript)
          |
Jakarta Servlet endpoints
          |
JDBC / MySQL stored procedures
          |
        MySQL
```

The initial tests are characterization tests: they protect existing endpoint behavior while application boundaries are introduced during migration.

## Local database

Requirements: Docker with Compose support.

1. Copy `.env.example` to `.env` and replace all placeholder passwords.
2. Start MySQL with `docker compose up -d mysql`.
3. Wait for `docker compose ps` to report the database as healthy.

The first startup creates the schema, stored procedure, and deterministic development data. The named Docker volume preserves the database. To intentionally rebuild it from scratch, remove the volume explicitly before starting MySQL again.

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

The Tomcat JNDI datasource currently remains part of the legacy deployment. It will be replaced by Spring Boot datasource configuration during the vertical-slice migration.

## Spring Boot migration scaffold

The Spring Boot application is introduced alongside the legacy WAR so endpoints can move one vertical slice at a time. Start the Docker database first, then run:

```shell
mvn spring-boot:run
```

The scaffold listens on `FABFLIX_BOOT_PORT` or `8081` by default. Its first endpoint is:

```text
GET http://localhost:8081/api/boot/health
```

## Tests and coverage

Run the complete verification lifecycle with:

```shell
mvn verify
```

CI enforces a **57% non-regression line-coverage baseline** for the legacy online web application (measured baseline: 57.41%). One-off XML parsers, importer worker/tasks, password initialization programs, and index-maintenance utilities are deliberately excluded because they are not part of the deployed request path. Their exclusion is defined centrally in `pom.xml`; new online application classes are covered by default.

The baseline should be ratcheted upward as legacy slices are improved. New Spring Boot application code will start with an 85% line-coverage target rather than inheriting the lower legacy baseline.

JaCoCo HTML reports are generated under `target/site/jacoco/`.

## Security note

The repository previously contained development database credentials and a reCAPTCHA secret. Those credentials must be considered compromised and rotated in their respective services; removing them from the current source does not invalidate copies in Git history.
