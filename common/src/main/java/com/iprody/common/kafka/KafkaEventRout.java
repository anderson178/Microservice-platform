package com.iprody.common.kafka;

public final class KafkaEventRout {
    private KafkaEventRout() {
    }

    public static final String PAYMENT_REQUEST = "payment.request";
    public static final String PAYMENT_RESPONSE = "payment.response";
    public static final String BANKING_REQUEST = "banking.request";
    public static final String BANKING_RESPONSE = "banking.response";
}
