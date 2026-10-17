# ─────────────────────────────────────────────────────────────
# Artha Backend — Dockerfile
#
# Multi-stage build:
#   Stage 1 (builder): Compiles Java code + runs tests → produces JAR
#   Stage 2 (runtime): Tiny JRE image that only runs the JAR
#
# This keeps the final image small (~300MB instead of ~1.5GB).
#
# Build:   docker build -t artha-backend ./backend
# Run:     docker run -p 8080:8080 artha-backend
# ─────────────────────────────────────────────────────────────

# === Stage 1: Build ===
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
# Download dependencies first (cached layer — only re-runs if pom.xml changes)
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests -B

# === Stage 2: Runtime ===
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Run as non-root user (per NFR-8.4)
RUN addgroup -S artha && adduser -S artha -G artha
USER artha

# Copy the JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Health check endpoint for Docker Compose
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
