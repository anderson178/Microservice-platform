# Платежный сервис
Платежный сервис выполняет роль посредника между Сервисом заказов и внешним Платежным провайдером. Он получает запросы на платежи из Kafka , инициирует фактическую оплату через внешний REST API и использует Kafka как временное хранилище состояния платежей до завершения обработки.

**Технический стек:**
+	Java 17
+	Maven 3.9.11
+	Spring Boot 4.0.5
+	PostgreSQL 16

# Сборка maven
```
mvn clean package -P payment-service
```

# Build and Run
**Сборка и запуск образов**<br>
Запуск из корня проекта
```
docker compose up -d --build payment-service
docker compose up -d --build payment-postgres
docker compose up -d --build payment-keycloak
```