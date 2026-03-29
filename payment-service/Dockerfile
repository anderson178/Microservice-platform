FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Xmx512m -Xms256m"

# nTLS
EXPOSE 8080
# TLS
EXPOSE 8443
# debug port
EXPOSE 8005

ENTRYPOINT ["java", "-jar", "app.jar"]