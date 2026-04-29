package com.iprody.payment.service;

import com.iprody.common.kafka.PaymentRequest;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.payment.mapper.PaymentMapper;
import com.iprody.payment.model.outbox.OutboxAggregateType;
import com.iprody.payment.model.outbox.OutboxEventType;
import com.iprody.payment.model.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessorService {
    private final PaymentService paymentService;
    private final OutboxEventService outboxEventService;

    @Transactional
    public void handlePayment(PaymentRequest event) {
        UUID inquiryRefId = event.getInquiryRefId();

        if (paymentService.existsByInquiryRefId(inquiryRefId)) {
            log.info("Payment with inquiryRefId={} already exists, skipping", inquiryRefId);
            return;
        }

        PaymentStatus paymentStatus = PaymentStatus.RECEIVED;
        Payment payment = PaymentMapper.INSTANCE.toPaymentRequest(event);
        payment.setPaymentStatus(paymentStatus);
        paymentService.save(payment);

        event.setStatus(paymentStatus);
        saveOutboxEvent(event);
    }

    private void saveOutboxEvent(PaymentRequest request) {
        outboxEventService.saveEvent(
                OutboxAggregateType.INQUIRY,
                request.getInquiryRefId(),
                OutboxEventType.PAYMENT_REQUESTED,
                request
        );
    }
}
