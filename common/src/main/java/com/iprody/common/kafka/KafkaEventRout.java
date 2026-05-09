package com.iprody.common.kafka;

public final class KafkaEventRout {
    private KafkaEventRout() {
    }

    public static final String PAYMENT_REQUEST = "payment.request";
    public static final String PAYMENT_RESPONSE = "payment.response";
    public static final String BANKING_REQUEST = "banking.request";
    public static final String BANKING_RESPONSE = "banking.response";
    public static final String CANCELLATION_REQUEST = "cancellation.request";
    public static final String CANCELLATION_RESPONSE = "cancellation.response";
    public static final String INVENTORY_REQUEST = "inventory.request";
    public static final String INVENTORY_RESPONSE = "inventory.response";
}
