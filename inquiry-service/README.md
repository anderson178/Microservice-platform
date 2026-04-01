# Платежный сервис
Inquiry Service предназначен для принятия и обработки всех входящих запросов от клиентов на приобретение и исследование продукта, и является центром хранения данных для эффективной работы отдела маркетинга и продаж.
**Технический стек:**
+	Java 17
+	Maven 3.9.11
+	Spring Boot 4.0.5
+	PostgreSQL 16

# Сборка maven
```
mvn clean package -P inquiry-service
```

# Build and Run
**Сборка и запуск образов**<br>
Запуск из корня проекта
```
docker compose up -d --build inquiry-service
docker compose up -d --build inquiry-postgres
docker compose up -d --build inquiry-keycloak
```