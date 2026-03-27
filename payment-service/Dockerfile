FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Xmx512m -Xms256m"

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]