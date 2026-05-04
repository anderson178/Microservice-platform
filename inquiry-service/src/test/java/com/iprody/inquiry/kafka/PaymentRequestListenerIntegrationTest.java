package com.iprody.inquiry.kafka;

import com.iprody.common.kafka.PaymentResponse;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.inquiry.configuration.KafkaTestConfig;
import com.iprody.inquiry.configuration.PostgresTestConfig;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryStatus;
import com.iprody.inquiry.repository.InquiryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

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
    private InquiryRepo inquiryRepo;

    @BeforeEach
    void setUp() {
        inquiryRepo.deleteAll();
    }

    @Nested
    @DisplayName("Positive")
    class PositiveTests {

        @Test
        @DisplayName("should process SUCCESS payment response and update Inquiry status to PAYMENT")
        void consume_successPaymentResponse_updatesInquiryStatus() {
            UUID inquiryId = inquiryRepo.save(createInquiry()).getId();

            PaymentResponse response = new PaymentResponse();
            response.setInquiryRefId(inquiryId);
            response.setStatus(PaymentStatus.RECEIVED);
            response.setReason("Payment processed");

            kafkaTemplate.send("payment.response", inquiryId.toString(), response);
            kafkaTemplate.flush();

            await()
                    .atMost(20, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Inquiry updatedInquiry = inquiryRepo.findById(inquiryId)
                                .orElseThrow(() -> new AssertionError("Inquiry not found"));

                        assertThat(updatedInquiry.getStatus()).isEqualTo(InquiryStatus.PAYMENT);
                        assertThat(updatedInquiry.getNote()).isNull();
                    });
        }

        @Test
        @DisplayName("should process FAILED payment response and update Inquiry note")
        void consume_failedPaymentResponse_updatesInquiryNote() {
            UUID inquiryId = inquiryRepo.save(createInquiry()).getId();

            PaymentResponse response = new PaymentResponse();
            response.setInquiryRefId(inquiryId);
            response.setStatus(PaymentStatus.NOT_SENT);
            response.setReason("Insufficient funds");

            kafkaTemplate.send("payment.response", inquiryId.toString(), response);
            kafkaTemplate.flush();

            await()
                    .atMost(20, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Inquiry updatedInquiry = inquiryRepo.findById(inquiryId)
                                .orElseThrow(() -> new AssertionError("Inquiry not found"));

                        assertThat(updatedInquiry.getStatus()).isNotEqualTo(InquiryStatus.PAYMENT);
                        assertThat(updatedInquiry.getNote())
                                .contains("Payment rejected by external service: Insufficient funds");
                    });
        }
    }

    @Nested
    @DisplayName("Negative")
    class NegativeTests {

        @Test
        @DisplayName("should not process when inquiryRefId is null (violates @NotNull)")
        void consume_nullInquiryRefId_doesNotUpdate() {
            UUID inquiryId = inquiryRepo.save(createInquiry()).getId();

            PaymentResponse response = new PaymentResponse();
            response.setStatus(PaymentStatus.RECEIVED);

            kafkaTemplate.send("payment.response", UUID.randomUUID().toString(), response);
            kafkaTemplate.flush();

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Inquiry unchangedInquiry = inquiryRepo.findById(inquiryId)
                                .orElseThrow();
                        assertThat(unchangedInquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
                    });
        }

        @Test
        @DisplayName("should not process when status is null (violates @NotNull)")
        void consume_nullStatus_doesNotUpdate() {
            UUID inquiryId = inquiryRepo.save(createInquiry()).getId();

            PaymentResponse response = new PaymentResponse();
            response.setInquiryRefId(inquiryId);
            response.setReason("Test");

            kafkaTemplate.send("payment.response", inquiryId.toString(), response);
            kafkaTemplate.flush();

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        Inquiry unchangedInquiry = inquiryRepo.findById(inquiryId)
                                .orElseThrow();
                        assertThat(unchangedInquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
                    });
        }

        @Test
        @DisplayName("should handle non-existent inquiry gracefully (no crash)")
        void consume_nonExistentInquiry_doesNotCrash() {
            UUID nonExistentInquiryId = UUID.randomUUID();

            PaymentResponse response = new PaymentResponse();
            response.setInquiryRefId(nonExistentInquiryId);
            response.setStatus(PaymentStatus.RECEIVED);

            kafkaTemplate.send("payment.response", nonExistentInquiryId.toString(), response);
            kafkaTemplate.flush();

            await()
                    .atMost(5, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        assertThat(inquiryRepo.findById(nonExistentInquiryId)).isEmpty();
                    });
        }
    }

    private Inquiry createInquiry() {
        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerRefId(UUID.randomUUID());
        inquiry.setManagerRefId(UUID.randomUUID());
        inquiry.setProductRefId(UUID.randomUUID());
        inquiry.setStatus(InquiryStatus.IN_PROGRESS);
        inquiry.setSource("web");

        return inquiry;
    }
}
