package com.iprody.payment.model.outbox;

import lombok.Getter;

@Getter
public enum OutboxEventType {
    PAYMENT_REQUESTED("payment.request", "payment.response");

    private final String listenTopic;
    private final String publishTopic;

    OutboxEventType(String listenTopic, String publishTopic) {
        this.listenTopic = listenTopic;
        this.publishTopic = publishTopic;
    }
}
