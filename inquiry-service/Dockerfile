FROM maven:3.9.11-eclipse-temurin-17-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY common/src common/src
COPY inquiry-service/pom.xml inquiry-service/
COPY inquiry-service/src inquiry-service/src
COPY checkstyle/sun_checks.xml checkstyle/

RUN mvn -B dependency:go-offline -pl inquiry-service -am -P inquiry-service -f pom.xml
RUN mvn clean package -DskipTests -pl inquiry-service -am -P inquiry-service -f pom.xml

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
