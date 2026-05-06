package com.iprody.inventory.configuration;

import com.iprody.inventory.model.OutboxEventType;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestConfig {
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.2.0"));

    static {
        KAFKA.start();
    }

    @Bean
    public DynamicPropertyRegistrar kafkaProperties() {
        return registry -> {
            registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        };
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return KAFKA;
    }

    @Bean
    public NewTopic cancellationRequestTopic() {
        return new NewTopic(OutboxEventType.CANCELLATION_REQUEST.getTopic(), 1, (short) 1);
    }

    @Bean
    public NewTopic cancellationResponseTopic() {
        return new NewTopic(OutboxEventType.CANCELLATION_RESPONSE.getTopic(), 1, (short) 1);
    }

    @Bean
    public NewTopic availabilityRequestTopic() {
        return new NewTopic(OutboxEventType.INVENTORY_AVAILABILITY_REQUEST.getTopic(), 1, (short) 1);
    }

    @Bean
    public NewTopic availabilityResponseTopic() {
        return new NewTopic(OutboxEventType.INVENTORY_AVAILABILITY_RESPONSE.getTopic(), 1, (short) 1);
    }
}
