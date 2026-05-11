package com.iprody.inquiry.model;

import com.iprody.common.kafka.KafkaEventRout;
import lombok.Getter;

@Getter
public enum OutboxEventType {
    CANCELLATION_REQUEST(KafkaEventRout.CANCELLATION_REQUEST),
    CANCELLATION_RESPONSE(KafkaEventRout.CANCELLATION_RESPONSE),
    INVENTORY_AVAILABILITY_REQUEST(KafkaEventRout.INVENTORY_AVAILABILITY_REQUEST),
    INVENTORY_AVAILABILITY_RESPONSE(KafkaEventRout.INVENTORY_AVAILABILITY_RESPONSE);

    private final String topic;

    OutboxEventType(String topic) {
        this.topic = topic;
    }
}
