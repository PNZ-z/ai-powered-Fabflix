# syntax=docker/dockerfile:1

# 1) Build the React single-page app.
FROM node:22-alpine AS frontend
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build && test -f dist/index.html

# 2) Build the Spring Boot jar with the frontend profile, caching Maven deps.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -Pfrontend dependency:go-offline
COPY src ./src
COPY --from=frontend /workspace/frontend/dist ./frontend/dist
RUN mvn -B -Pfrontend -DskipTests clean package

# 3) Run the packaged application as an unprivileged user.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd --system --gid 1001 fabflix \
    && useradd --system --uid 1001 --gid fabflix --no-create-home fabflix
COPY --from=build --chown=fabflix:fabflix /workspace/target/fablix.jar ./fablix.jar
USER fabflix
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/fablix.jar"]
