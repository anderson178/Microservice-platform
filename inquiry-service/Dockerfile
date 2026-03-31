FROM maven:3.9.11-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY inquiry-service/pom.xml inquiry-service/
COPY inquiry-service/src inquiry-service/src
COPY checkstyle/sun_checks.xml checkstyle/

WORKDIR /app/inquiry-service

RUN mvn -B dependency:go-offline

RUN mvn clean package -DskipTests -P inquiry-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/inquiry-service/target/*.jar app.jar

# nTLS
EXPOSE 8080
# TLS
EXPOSE 8443
# debug port
EXPOSE 5005

ENTRYPOINT ["java", "-jar", "app.jar"]