# Stage 1: Build the application with Maven
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Create the final image with the built jar and necessary dependencies
FROM eclipse-temurin:21
COPY --from=build /target/demo-0.0.1-SNAPSHOT.jar demo.jar

# Install Chrome and ChromeDriver
RUN apt-get update && \
    apt-get install -y wget gnupg && \
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list' && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    CHROMEDRIVER_VERSION=`curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE` && \
    wget -N http://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip && \
    unzip chromedriver_linux64.zip && \
    chmod +x chromedriver && \
    mv -f chromedriver /usr/local/bin/chromedriver && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/*

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "demo.jar"]