package com.iprody.inventory.kafka.event;

import com.iprody.common.kafka.CancellationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated
public class CancellationEventPublisher {
    private static final String TOPIC = "cancellation.response";

    private final KafkaTemplate<String, CancellationResponse> cancellationKafkaTemplate;

    public void publish(CancellationResponse event) {
        String key = event.getId().toString();
        log.info("Publishing cancellation with recors group id={}", key);
        cancellationKafkaTemplate
                .send(new ProducerRecord<>(TOPIC, key, event))
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published successfully. Offset: {}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish: {}", ex.getMessage(), ex);
                    }
                });
    }
}
