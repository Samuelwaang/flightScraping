# Stage 1: Build the application with Maven
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Create the final image with the built jar and necessary dependencies
FROM eclipse-temurin:17

# Install dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    xvfb \
    libxi6 \
    # libgconf-2-4 \
    libnss3 \
    libfontconfig1 \
    libxcb1 \
    gnupg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Chrome
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

RUN google-chrome --version

# Remove ChromeDriver installation steps

# Add your application jar to the container
COPY --from=build /target/demo-0.0.1-SNAPSHOT.jar /app/demo.jar

# Expose the port the application runs on
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Dwebdriver.chrome.whitelistedIps="
ENV CHROME_BIN=/usr/bin/google-chrome

# Create a non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Change ownership of the application directory
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Set the working directory
WORKDIR /app

# Run the jar file
ENTRYPOINT ["java", "-jar", "demo.jar"]
