package com.iprody.payment.service;

import com.iprody.common.kafka.PaymentRequest;
import com.iprody.common.kafka.PaymentResponse;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.payment.kafka.dto.BankRequest;
import com.iprody.payment.kafka.dto.BankResponse;
import com.iprody.payment.mapper.PaymentMapper;
import com.iprody.payment.model.outbox.OutboxAggregateType;
import com.iprody.payment.model.outbox.OutboxEventType;
import com.iprody.payment.model.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        saveOutboxEvent(event, OutboxEventType.PAYMENT_RESPONSE);
        saveOutboxEvent(event, OutboxEventType.BANKING_REQUEST);
    }

    private void saveOutboxEvent(PaymentRequest request, OutboxEventType eventType) {
        outboxEventService.saveEvent(
                OutboxAggregateType.INQUIRY,
                request.getInquiryRefId(),
                eventType,
                request
        );
    }

    @Transactional
    public void responseBankInitialStageProcessing(UUID inquiryRefId, BankResponse response) {
        paymentService.updateByInquiryRefId(inquiryRefId, PaymentStatus.PENDING, response.getId());
        outboxEventService.saveEvent(
                OutboxAggregateType.PAYMENT,
                inquiryRefId,
                OutboxEventType.BANKING_RESPONSE,
                response
        );
    }

    @Transactional
    public void responseBankProcessing(UUID inquiryRefId, BankResponse response) {
        PaymentStatus paymentStatus = response.getStatus().equals(BankResponse.Status.SUCCEEDED)
                ? PaymentStatus.APPROVED
                : PaymentStatus.REJECTED;

        paymentService.updateByInquiryRefId(inquiryRefId, paymentStatus, response.getId());
        outboxEventService.saveEvent(
                OutboxAggregateType.INQUIRY,
                inquiryRefId,
                OutboxEventType.PAYMENT_RESPONSE,
                new PaymentResponse(
                        inquiryRefId,
                        response.getAmount(),
                        response.getCurrency().getCurrencyCode(),
                        paymentStatus,
                        ""
                )
        );
    }

    @Transactional
    public void bankErrorHandle(UUID inquiryRefId, PaymentStatus paymentStatus, BankResponse bankResponse) {
        bankErrorHandle(inquiryRefId, paymentStatus, bankResponse.getAmount(), bankResponse.getCurrency().getCurrencyCode());
    }

    @Transactional
    public void bankErrorHandle(UUID inquiryRefId, PaymentStatus paymentStatus, BankRequest bankRequest) {
        bankErrorHandle(inquiryRefId, paymentStatus, bankRequest.getAmount(), bankRequest.getCurrency());
    }

    @Transactional
    public void bankErrorHandle(UUID inquiryRefId, PaymentStatus paymentStatus, BigDecimal amount, String currency) {
        paymentService.updateByInquiryRefId(inquiryRefId, paymentStatus);
        outboxEventService.saveEvent(
                OutboxAggregateType.INQUIRY,
                inquiryRefId,
                OutboxEventType.PAYMENT_RESPONSE,
                new PaymentResponse(
                        inquiryRefId,
                        amount,
                        currency,
                        paymentStatus,
                        "All retry attempts failed"
                )
        );
    }
}
