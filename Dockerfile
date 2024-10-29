# Start with an official Java image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory in the Docker container
WORKDIR /app

# Copy the built application JAR file from the target folder to the app folder in the container
ADD  target/pdfmaker-0.0.1-SNAPSHOT.jar app.jar

# Expose the port on which the app will run
EXPOSE 8080

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
