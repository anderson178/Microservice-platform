package com.iprody.common.struct;

public enum PaymentStatus {
    /**
     * The payment request has been received, but has not yet been sent to the financial institution for processing.
     */
    RECEIVED,
    /**
     * The payment request has been received and sent to the financial institution for processing.
     */
    PENDING,
    /**
     * A response has been received from the financial institution, the request has been processed.
     */
    DECLINED,
    /**
     * A response has been received from the financial institution, the request has been processed.
     */
    APPROVED,
    /**
     * The payment request has been received, but attempts to send it to the financial institution for processing were unsuccessful.
     */
    NOT_SENT,
    /**
     * The request was rejected by the service
     */
    REJECTED
}
