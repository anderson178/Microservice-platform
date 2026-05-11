package com.iprody.inventory.model;

import com.iprody.common.kafka.KafkaEventRout;
import lombok.Getter;

@Getter
public enum OutboxEventType {
    CANCELLATION_REQUEST(KafkaEventRout.CANCELLATION_REQUEST),
    CANCELLATION_RESPONSE(KafkaEventRout.CANCELLATION_RESPONSE),
    INVENTORY_REQUEST(KafkaEventRout.INVENTORY_REQUEST),
    INVENTORY_RESPONSE(KafkaEventRout.INVENTORY_RESPONSE);

    private final String topic;

    OutboxEventType(String topic) {
        this.topic = topic;
    }
}
