package com.iprody.payment.model.outbox;

import com.iprody.common.kafka.KafkaEventRout;
import lombok.Getter;

@Getter
public enum OutboxEventType {
    PAYMENT_REQUEST(KafkaEventRout.PAYMENT_REQUEST),
    PAYMENT_RESPONSE(KafkaEventRout.PAYMENT_RESPONSE),
    BANKING_REQUEST(KafkaEventRout.BANKING_REQUEST),
    BANKING_RESPONSE(KafkaEventRout.BANKING_RESPONSE);

    private final String topic;

    OutboxEventType(String topic) {
        this.topic = topic;
    }
}
