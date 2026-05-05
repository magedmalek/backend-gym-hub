# ---- Build Stage ----
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Cache dependencies first (only re-runs if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN groupadd spring && useradd -g spring spring
USER spring

# Copy the jar from builder
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
