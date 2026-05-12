package com.iprody.inquiry.model;

import com.iprody.common.kafka.KafkaEventTopic;
import lombok.Getter;

@Getter
public enum OutboxEventType {
    CANCELLATION_REQUEST(KafkaEventTopic.CANCELLATION_REQUEST),
    CANCELLATION_RESPONSE(KafkaEventTopic.CANCELLATION_RESPONSE),
    INVENTORY_AVAILABILITY_REQUEST(KafkaEventTopic.INVENTORY_AVAILABILITY_REQUEST),
    INVENTORY_AVAILABILITY_RESPONSE(KafkaEventTopic.INVENTORY_AVAILABILITY_RESPONSE);

    private final String topic;

    OutboxEventType(String topic) {
        this.topic = topic;
    }
}
