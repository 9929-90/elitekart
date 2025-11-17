# ============================
# 1. Build Stage
# ============================
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies first (layer caching)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copy application source
COPY src ./src

# Build the application
RUN mvn -q clean package -DskipTests

# ============================
# 2. Runtime Stage
# ============================
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose application port
EXPOSE 8081

# Run application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
