package com.iprody.inquiry.model;

public enum OutboxEventStatus {
    /**
     * Event is awaiting publication.
     */
    PENDING,

    /**
     * Event was successfully submitted to Kafka.
     */
    PUBLISHED,

    /**
     * An error occurred during publishing.
     */
    FAILED
}
