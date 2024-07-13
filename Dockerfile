FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21
RUN apt-get update && \
    apt-get install -y wget unzip && \
    apt-get install -y libgbm-dev libxshmfence1 && \
    apt-get install -y chromium-driver

COPY --from=build /target/demo-0.0.1-SNAPSHOT.jar demo.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","demo.jar"]