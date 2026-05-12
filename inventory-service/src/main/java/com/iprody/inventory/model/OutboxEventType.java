package com.iprody.inventory.model;

import com.iprody.common.kafka.KafkaEventTopic;
import lombok.Getter;

@Getter
public enum OutboxEventType {
    CANCELLATION_REQUEST(KafkaEventTopic.CANCELLATION_REQUEST),
    CANCELLATION_RESPONSE(KafkaEventTopic.CANCELLATION_RESPONSE),
    INVENTORY_REQUEST(KafkaEventTopic.INVENTORY_REQUEST),
    INVENTORY_RESPONSE(KafkaEventTopic.INVENTORY_RESPONSE);

    private final String topic;

    OutboxEventType(String topic) {
        this.topic = topic;
    }
}
