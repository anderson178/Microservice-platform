# Сервис клиентов
Сервис клиентов предназначен для хранения данных о покупателях, включая контактные и платежные реквизиты, а также предоставления REST API для CRUD-операций над этими данными.

**Технический стек:**
+	Java 17
+	Maven 3.9.11
+	Spring Boot 4.0.5
+	PostgreSQL 16

# Сборка maven
```
mvn clean package -P customer-service
```

# Build and Run
**Сборка и запуск образов**<br>
Запуск из корня проекта
```
docker compose up -d --build payment-service
docker compose up -d --build payment-postgres
docker compose up -d --build payment-keycloak
```