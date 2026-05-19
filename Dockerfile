# Use official Eclipse Temurin image for Java 21
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the packaged jar file into our docker image
COPY target/*.jar app.jar

# Expose the port Cloud Run expects
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java","-jar","app.jar"]