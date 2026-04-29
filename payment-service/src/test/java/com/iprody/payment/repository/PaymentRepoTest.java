package com.iprody.payment.repository;

import com.iprody.common.struct.PaymentStatus;
import com.iprody.payment.configuration.PostgresTestConfig;
import com.iprody.payment.model.payment.Payment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(PostgresTestConfig.class)
class PaymentRepoTest {
    @Autowired
    private PaymentRepo paymentRepo;

    @PersistenceContext
    private EntityManager entityManager;

    private final UUID testInquiryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentRepo.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Positive")
    class PositiveTests {

        @Test
        @DisplayName("should find payments by Status and InquiryRefId")
        void shouldFindByStatusAndInquiry() {
            createPayment(testInquiryId, new BigDecimal("100.00"), "USD",
                    PaymentStatus.PENDING, Timestamp.valueOf("2024-01-01 10:00:00"));
            createPayment(testInquiryId, new BigDecimal("150.00"), "USD",
                    PaymentStatus.APPROVED, Timestamp.valueOf("2024-01-02 10:00:00"));
            createPayment(UUID.randomUUID(), new BigDecimal("200.00"), "EUR",
                    PaymentStatus.PENDING, Timestamp.valueOf("2024-01-03 10:00:00"));

            Page<Payment> result = paymentRepo.findAllByFilter(
                    null, null, null, testInquiryId, PaymentStatus.PENDING, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            Payment found = result.getContent().get(0);
            assertThat(found.getInquiryRefId()).isEqualTo(testInquiryId);
            assertThat(found.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(found.getAmount()).isEqualTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should return all payments when all filters are null")
        void shouldReturnAllWhenFiltersAreNull() {
            createPayment(testInquiryId, new BigDecimal("100.00"), "USD",
                    PaymentStatus.PENDING, Timestamp.valueOf("2024-01-01 10:00:00"));
            createPayment(UUID.randomUUID(), new BigDecimal("200.00"), "EUR",
                    PaymentStatus.APPROVED, Timestamp.valueOf("2024-01-02 10:00:00"));
            createPayment(UUID.randomUUID(), new BigDecimal("300.00"), "GBP",
                    PaymentStatus.NOT_SENT, Timestamp.valueOf("2024-01-03 10:00:00"));

            Page<Payment> result = paymentRepo.findAllByFilter(
                    null, null, null, null, null, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getContent())
                    .extracting(Payment::getAmount)
                    .containsExactlyInAnyOrder(
                            new BigDecimal("100.00"),
                            new BigDecimal("200.00"),
                            new BigDecimal("300.00")
                    );
        }

        @Test
        @DisplayName("should apply pagination correctly")
        void shouldApplyPagination() {
            for (int i = 0; i < 10; i++) {
                createPayment(UUID.randomUUID(), new BigDecimal(i + ".00"), "USD",
                        PaymentStatus.PENDING, Timestamp.valueOf("2024-01-01 10:00:00"));
            }

            Page<Payment> result = paymentRepo.findAllByFilter(
                    null, null, null, null, null, PageRequest.of(1, 3));

            assertThat(result.getTotalElements()).isEqualTo(10);
            assertThat(result.getTotalPages()).isEqualTo(4);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Negative")
    class NegativeTests {

        @Test
        @DisplayName("should return empty page when no payments match date range")
        void shouldReturnEmptyWhenNoPaymentsInDateRange() {
            createPayment(testInquiryId, new BigDecimal("100.00"), "USD",
                    PaymentStatus.PENDING, Timestamp.valueOf("2023-01-01 10:00:00"));
            createPayment(UUID.randomUUID(), new BigDecimal("200.00"), "EUR",
                    PaymentStatus.APPROVED, Timestamp.valueOf("2025-01-01 10:00:00"));

            Timestamp from = Timestamp.valueOf("2024-01-01 00:00:00");
            Timestamp to = Timestamp.valueOf("2024-12-31 23:59:59");

            Page<Payment> result = paymentRepo.findAllByFilter(
                    from, to, null, null, null, PageRequest.of(0, 10));

            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should return empty page when inquiryRefId does not exist")
        void shouldReturnEmptyWhenInquiryRefIdNotFound() {
            createPayment(UUID.randomUUID(), new BigDecimal("100.00"), "USD",
                    PaymentStatus.PENDING, Timestamp.valueOf("2024-01-01 10:00:00"));

            UUID nonExistentInquiryId = UUID.randomUUID();

            Page<Payment> result = paymentRepo.findAllByFilter(
                    null, null, null, nonExistentInquiryId, null, PageRequest.of(0, 10));

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should return empty page when status does not match")
        void shouldReturnEmptyWhenStatusNotFound() {
            createPayment(testInquiryId, new BigDecimal("100.00"), "USD",
                    PaymentStatus.PENDING, Timestamp.valueOf("2024-01-01 10:00:00"));

            Page<Payment> result = paymentRepo.findAllByFilter(
                    null, null, null, null, PaymentStatus.APPROVED, PageRequest.of(0, 10));

            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should handle null date range gracefully (ignore date filter)")
        void shouldIgnoreNullDateRange() {
            createPayment(testInquiryId, new BigDecimal("100.00"), "USD",
                    PaymentStatus.PENDING, Timestamp.valueOf("2020-01-01 10:00:00"));
            createPayment(UUID.randomUUID(), new BigDecimal("200.00"), "EUR",
                    PaymentStatus.APPROVED, Timestamp.valueOf("2030-01-01 10:00:00"));

            Page<Payment> result = paymentRepo.findAllByFilter(
                    null, null, null, null, null, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle invalid page parameters gracefully")
        void shouldHandleInvalidPageParameters() {
            createPayment(testInquiryId, new BigDecimal("100.00"), "USD",
                    PaymentStatus.PENDING, Timestamp.valueOf("2024-01-01 10:00:00"));
            createPayment(UUID.randomUUID(), new BigDecimal("200.00"), "EUR",
                    PaymentStatus.APPROVED, Timestamp.valueOf("2024-01-02 10:00:00"));

            Page<Payment> result = paymentRepo.findAllByFilter(
                    null, null, null, null, null, PageRequest.of(10, 10));

            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getNumber()).isEqualTo(10);
            assertThat(result.getTotalPages()).isEqualTo(1);
        }
    }

    private void createPayment(UUID inquiryRefId, BigDecimal amount, String currency, PaymentStatus status, Timestamp createdAt) {
        Payment payment = new Payment();
        payment.setInquiryRefId(inquiryRefId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setPaymentStatus(status);
        payment.setCreatedAt(createdAt);
        payment.setUpdatedAt(createdAt);

        entityManager.persist(payment);
        entityManager.flush();
        entityManager.clear();
    }
}
