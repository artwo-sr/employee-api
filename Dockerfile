# =============================================================================
# Multi-stage Dockerfile — Employee Microservice
# =============================================================================
# Stage 1 : Build  — full JDK + Maven, creates the executable JAR.
# Stage 2 : Runtime — minimal JRE Alpine image, copies only the JAR.
#
# Multi-stage benefits:
#   - The final image does NOT contain Maven, source code, or build caches.
#   - Alpine base keeps the runtime image under ~100 MB.
#   - A non-root user is created at runtime to follow least-privilege principle.
# =============================================================================

# -----------------------------------------------------------------------------
# STAGE 1 — Build
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

LABEL stage="build"

WORKDIR /app

# Copy POM first and resolve dependencies in a separate layer.
# This layer is cached and only invalidated when pom.xml changes,
# dramatically speeding up subsequent builds when only source changes.
COPY pom.xml .
RUN mvn dependency:go-offline --batch-mode --no-transfer-progress

# Copy source and build the fat JAR, skipping tests (tests run in CI).
COPY src ./src
RUN mvn clean package \
        --batch-mode \
        --no-transfer-progress \
        -DskipTests

# -----------------------------------------------------------------------------
# STAGE 2 — Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="backend-team@enterprise.com" \
      description="Enterprise Employee Management REST API Microservice" \
      version="1.0.0"

WORKDIR /app

# Create a dedicated non-root user and group for OWASP least-privilege compliance.
# Running as root inside a container is an OWASP A05 (Security Misconfiguration) risk.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create log directory with proper ownership so the app can write rolling logs.
RUN mkdir -p logs && chown appuser:appgroup logs

# Copy only the JAR from the build stage — no source code, no Maven cache.
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appgroup app.jar

# Switch to non-root user before runtime
USER appuser

# Expose the default application port (matches server.port in application.yml)
EXPOSE 8080

# JVM tuning flags:
#   -XX:+UseContainerSupport               — respects Docker CPU/memory limits
#   -XX:MaxRAMPercentage=75.0              — use 75% of container RAM for heap
#   -Djava.security.egd=file:/dev/./urandom — faster SecureRandom initialisation in containers
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
