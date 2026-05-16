package com.iprody.payment.health;

import com.iprody.common.kafka.KafkaEventTopic;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component("kafka")
public class KafkaHealthIndicator implements HealthIndicator {
    private final AdminClient adminClient;
    private final Set<String> requiredTopics = Set.of(
            KafkaEventTopic.PAYMENT_REQUEST,
            KafkaEventTopic.PAYMENT_RESPONSE,
            KafkaEventTopic.BANKING_REQUEST,
            KafkaEventTopic.BANKING_RESPONSE
    );

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Override
    public Health health() {
        try {
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> existingTopics = topicsResult.names().get(3, TimeUnit.SECONDS);

            if (existingTopics.containsAll(requiredTopics)) {
                return Health.up()
                        .withDetail("cluster", "available")
                        .withDetail("topicsChecked", requiredTopics)
                        .build();
            } else {
                Set<String> missingTopics = new HashSet<>(requiredTopics);
                missingTopics.removeAll(existingTopics);

                return Health.down()
                        .withDetail("error", "Missing required topics")
                        .withDetail("missingTopics", missingTopics)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", "Kafka broker is unavailable")
                    .withException(e)
                    .build();
        }
    }
}
