# ╔════════════════════════════════════════════════════════════╗
# ║  BALANCE TRACKER - Multi-Stage Docker Build               ║
# ║  Spring Boot 4.0 + Java 25 + PostgreSQL                   ║
# ╚════════════════════════════════════════════════════════════╝

# ============================================================
# Stage 1: Builder - Compile and package the application
# ============================================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

# Copy Gradle wrapper and build files first (better layer caching)
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the application (skip tests for faster builds)
RUN ./gradlew clean build -x test --no-daemon

# ============================================================
# Stage 2: Runtime - Minimal image for running the application
# ============================================================
FROM eclipse-temurin:25-jdk

# JDK (not JRE) is required for keytool certificate generation
LABEL maintainer="Balance Tracker Development Team"
LABEL description="Balance Tracker - Personal Finance Management"

# Create non-root user for security (handle existing UID/GID gracefully)
RUN groupadd --gid 1001 balancetracker || true && \
    useradd --uid 1001 --gid 1001 --shell /bin/bash --create-home balancetracker || true

WORKDIR /app

# Create directories for logs and certificates
RUN mkdir -p /app/logs /app/certs && \
    chown -R balancetracker:balancetracker /app

# Copy the built JAR from builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

# Copy entrypoint script
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh && \
    chown balancetracker:balancetracker /app/docker-entrypoint.sh /app/app.jar

# Switch to non-root user
USER balancetracker

# Expose ports: HTTPS (8443) and HTTP (8080)
EXPOSE 8443 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f -k https://localhost:8443/balancetracker/actuator/health 2>/dev/null || \
        curl -f http://localhost:8080/balancetracker/actuator/health 2>/dev/null || exit 1

# Use entrypoint for certificate generation
ENTRYPOINT ["/app/docker-entrypoint.sh"]
