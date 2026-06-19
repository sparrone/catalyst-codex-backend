# Multi-stage build for optimized container size
# Stage 1: Build the application
FROM maven:3.9.8-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy

# Install SQLite for database operations and curl for health checks
RUN apt-get update && \
    apt-get install -y sqlite3 curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r catalystcodex && useradd -r -g catalystcodex catalystcodex

# Create application directory
WORKDIR /app

# Create data directory for SQLite database
RUN mkdir -p /app/data && chown -R catalystcodex:catalystcodex /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/catalyst-codex-backend-*.jar app.jar

# Copy pre-initialized SQLite database
COPY data/awakening-prod.db /app/data/awakening-prod.db

# Change ownership to non-root user
RUN chown -R catalystcodex:catalystcodex /app

# Switch to non-root user
USER catalystcodex

# Expose port (Cloud Run uses PORT environment variable)
EXPOSE 8080

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/api/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application with the pre-initialized database
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]