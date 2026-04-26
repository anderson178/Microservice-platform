package com.iprody.inventory.model;

import lombok.Getter;

@Getter
public enum OutboxEventType {
    CANCELLATION_REQUESTED("cancellation.request", "cancellation.response");

    private final String listenTopic;
    private final String publishTopic;

    OutboxEventType(String listenTopic, String publishTopic) {
        this.listenTopic = listenTopic;
        this.publishTopic = publishTopic;
    }
}
