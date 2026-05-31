# Stage 1: Build the Spring Boot JAR
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight runtime environment
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install native Tesseract OCR + English training data
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

# Copy the compiled JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Define storage directory for uploads
RUN mkdir -p uploads
VOLUME /app/uploads

# Expose backend port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
