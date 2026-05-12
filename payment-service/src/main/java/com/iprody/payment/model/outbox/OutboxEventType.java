package com.iprody.payment.model.outbox;

import com.iprody.common.kafka.KafkaEventTopic;
import lombok.Getter;

@Getter
public enum OutboxEventType {
    PAYMENT_REQUEST(KafkaEventTopic.PAYMENT_REQUEST),
    PAYMENT_RESPONSE(KafkaEventTopic.PAYMENT_RESPONSE),
    BANKING_REQUEST(KafkaEventTopic.BANKING_REQUEST),
    BANKING_RESPONSE(KafkaEventTopic.BANKING_RESPONSE);

    private final String topic;

    OutboxEventType(String topic) {
        this.topic = topic;
    }
}
