FROM maven:3.8.1-openjdk-11 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (leverages Docker layer caching)
COPY akka-cluster/pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY akka-cluster/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy built JAR from builder
COPY --from=builder /app/target/akka-cluster-app.jar .

# Copy entrypoint script
COPY akka-cluster/docker-entrypoint.sh .
RUN chmod +x docker-entrypoint.sh

# Expose ports for cluster communication
EXPOSE 2551 2552

# Set JVM options for running in container
ENV JVM_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled"

ENTRYPOINT ["./docker-entrypoint.sh"]
