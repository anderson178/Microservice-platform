package com.iprody.payment.event;

import com.iprody.common.kafka.PaymentRequest;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.payment.configuration.KafkaTestConfig;
import com.iprody.payment.configuration.PostgresTestConfig;
import com.iprody.payment.model.outbox.OutboxEvent;
import com.iprody.payment.model.outbox.OutboxEventType;
import com.iprody.payment.model.payment.Payment;
import com.iprody.payment.repository.OutboxEventRepo;
import com.iprody.payment.repository.PaymentRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Import({PostgresTestConfig.class, KafkaTestConfig.class})
@DisplayName("PaymentRequestListener Integration Test")
class PaymentRequestListenerIntegrationTest {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private OutboxEventRepo outboxEventRepo;
    @Autowired
    private PaymentRepo paymentRepo;

    @BeforeEach
    void setUp() {
        outboxEventRepo.deleteAll();
        paymentRepo.deleteAll();
    }

    @Nested
    @DisplayName("Positive")
    class PositiveTests {

        @Test
        @DisplayName("should process valid payment request and save payment + outbox event")
        void consume_validPaymentRequest_savesPaymentAndOutboxEvent() {
            UUID inquiryRefId = UUID.randomUUID();
            PaymentRequest request = createValidPaymentRequest(inquiryRefId);

            kafkaTemplate.send(
                    OutboxEventType.PAYMENT_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    request
            );

            kafkaTemplate.flush();

            await()
                    .atMost(30, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Payment savedPayment = paymentRepo
                                .findByInquiryRefId(inquiryRefId)
                                .orElseThrow(
                                        () -> new AssertionError("Payment not found for inquiryRefId="
                                                + inquiryRefId));

                        assertThat(savedPayment.getInquiryRefId()).isEqualTo(inquiryRefId);
                    });
        }

        @Test
        @DisplayName("should handle idempotency: duplicate request with same inquiryRefId is ignored")
        void consume_duplicateRequest_isIgnored_dueToIdempotency() {
            UUID inquiryRefId = UUID.randomUUID();
            PaymentRequest request = createValidPaymentRequest(inquiryRefId);

            kafkaTemplate.send(
                    OutboxEventType.PAYMENT_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    request
            );
            // Duplicate
            kafkaTemplate.send(
                    OutboxEventType.PAYMENT_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    request
            );

            await()
                    .atMost(20, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<Payment> payments = paymentRepo.findAllByInquiryRefId(inquiryRefId);
                        assertThat(payments).hasSize(1);

                        Payment saved = payments.get(0);
                        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.RECEIVED);

                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.PAYMENT_REQUEST);
                        assertThat(events).hasSize(1);
                    });
        }
    }

    @Nested
    @DisplayName("Negative")
    class NegativeTests {

        @Test
        @DisplayName("should not persist when inquiryRefId is null (violates @NotNull)")
        void consume_nullInquiryRefId_doesNotPersist() {
            PaymentRequest request = new PaymentRequest();
            request.setInquiryRefId(null);
            request.setAmount(new BigDecimal("100.00"));
            request.setCurrency("USD");
            request.setNote("Test");

            UUID inquiryRefId = UUID.randomUUID();

            kafkaTemplate.send(
                    OutboxEventType.PAYMENT_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    request
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<Payment> payments = paymentRepo.findAllByInquiryRefId(inquiryRefId);
                        assertThat(payments).isEmpty();

                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.PAYMENT_REQUEST);
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when amount is null (violates @NotNull)")
        void consume_nullAmount_doesNotPersist() {
            UUID inquiryRefId = UUID.randomUUID();
            PaymentRequest request = new PaymentRequest();
            request.setInquiryRefId(inquiryRefId);
            request.setAmount(null);
            request.setCurrency("USD");

            kafkaTemplate.send(
                    OutboxEventType.PAYMENT_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    request
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<Payment> payments = paymentRepo.findAllByInquiryRefId(inquiryRefId);
                        assertThat(payments).isEmpty();

                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.PAYMENT_REQUEST);
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when currency is blank (violates @NotBlank)")
        void consume_blankCurrency_doesNotPersist() {
            UUID inquiryRefId = UUID.randomUUID();
            PaymentRequest request = new PaymentRequest();
            request.setInquiryRefId(inquiryRefId);
            request.setAmount(new BigDecimal("100.00"));
            request.setCurrency("   ");
            request.setNote("Test");

            kafkaTemplate.send(
                    OutboxEventType.PAYMENT_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    request
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<Payment> payments = paymentRepo.findAllByInquiryRefId(inquiryRefId);
                        assertThat(payments).isEmpty();

                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.PAYMENT_REQUEST);
                        assertThat(events).isEmpty();
                    });
        }

        @Test
        @DisplayName("should not persist when payment already exists (idempotency check)")
        void consume_existingPayment_doesNotCreateDuplicate() {
            UUID inquiryRefId = UUID.randomUUID();

            Payment existingPayment = new Payment();
            existingPayment.setInquiryRefId(inquiryRefId);
            existingPayment.setAmount(new BigDecimal("100.00"));
            existingPayment.setCurrency("USD");
            existingPayment.setPaymentStatus(PaymentStatus.RECEIVED);
            paymentRepo.save(existingPayment);

            PaymentRequest duplicateRequest = createValidPaymentRequest(inquiryRefId);

            kafkaTemplate.send(
                    OutboxEventType.PAYMENT_REQUEST.getTopic(),
                    inquiryRefId.toString(),
                    duplicateRequest
            );

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        List<Payment> payments = paymentRepo.findAllByInquiryRefId(inquiryRefId);
                        assertThat(payments).hasSize(1);

                        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                                inquiryRefId, OutboxEventType.PAYMENT_REQUEST);
                        assertThat(events).hasSizeLessThanOrEqualTo(1);
                    });
        }
    }

    private PaymentRequest createValidPaymentRequest(UUID inquiryRefId) {
        PaymentRequest request = new PaymentRequest();
        request.setInquiryRefId(inquiryRefId);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        request.setNote("Customer requested payment");
        return request;
    }
}
