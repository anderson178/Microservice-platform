# Сервис бронирования

**Технический стек:**
+	Java 17
+	Maven 3.9.11
+	Spring Boot 4.0.5
+	PostgreSQL 16

# Сборка maven
```
mvn clean package -P inventory-service
```

# Build and Run
**Сборка и запуск образов**<br>
Запуск из корня проекта
```
docker compose up -d --build inventory-service
docker compose up -d --build inventory-postgres
docker compose up -d --build inventory-keycloak
```