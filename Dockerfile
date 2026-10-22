# ==============================================================================
# Multi-stage Dockerfile for Clinic Management System Backend
# ==============================================================================
# Stage 1: Build the Application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and POM first for efficient layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source code and package
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Create unprivileged system user for container security
RUN addgroup -S clinicgroup && adduser -S clinicuser -G clinicgroup

# Copy compiled executable JAR from builder stage
COPY --from=builder /app/target/clinic-management-*.jar app.jar
RUN chown -R clinicuser:clinicgroup /app

USER clinicuser

EXPOSE 8085

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="postgres"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
