package com.iprody.payment.kafka;

public class PaymentStillProcessingException extends RuntimeException {
    public PaymentStillProcessingException(String message) {
        super(message);
    }
}
