# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-22 as builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM openjdk:22-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/ChatApplication-1.0-SNAPSHOT.jar app.jar
EXPOSE 5000
CMD ["java", "-jar", "app.jar"]

