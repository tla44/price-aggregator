# Using a Gradle image that includes OpenJDK 17
FROM gradle:jdk21 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle wrapper files and the build scripts first
COPY gradlew .
COPY gradlew.bat .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy the source code
COPY src ./src

# Make the Gradle wrapper executable
RUN chmod +x gradlew

# Build the application using the Gradle wrapper
RUN ./gradlew build -x test

# Stage 2: Create the final runtime image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from the 'build' stage to the current stage
COPY --from=build /app/build/libs/*-fat.jar ./price-aggregator.jar
COPY conf ./conf

# Run the application.
CMD ["java", "-jar", "price-aggregator.jar"]
