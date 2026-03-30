FROM maven:3.9.11-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY payment-service/pom.xml payment-service/
COPY payment-service/src payment-service/src
COPY checkstyle/sun_checks.xml checkstyle/

WORKDIR /app/payment-service

RUN mvn -B dependency:go-offline

RUN mvn clean package -DskipTests -P payment-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/payment-service/target/*.jar app.jar

EXPOSE 8081
# nTLS
EXPOSE 8080
# TLS
EXPOSE 8443
# debug port
EXPOSE 8005
ENTRYPOINT ["java", "-jar", "app.jar"]