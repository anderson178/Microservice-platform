package com.iprody.payment.repository;

import com.iprody.payment.model.Payment;
import com.iprody.payment.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql(scripts = {"/sql/init-schema.sql"})
class PaymentRepoTest {
    @Container
    @ServiceConnection // Автоматически подставит url/username/password в Spring
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("sql/init-schema.sql");

    @Autowired
    private PaymentRepo paymentRepo;

    private final UUID testInquiryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        paymentRepo.deleteAll();

        Payment p1 = new Payment();
        p1.setInquiryRefId(testInquiryId);
        p1.setAmount(new BigDecimal("100.00"));
        p1.setCurrency("USD");
        p1.setPaymentStatus(PaymentStatus.PENDING);
        p1.setCreatedAt(Timestamp.valueOf("2024-01-01 10:00:00"));
        p1.setUpdatedAt(p1.getCreatedAt());

        Payment p2 = new Payment();
        p2.setInquiryRefId(UUID.randomUUID());
        p2.setAmount(new BigDecimal("200.00"));
        p2.setCurrency("EUR");
        p2.setPaymentStatus(PaymentStatus.APPROVED);
        p2.setCreatedAt(Timestamp.valueOf("2024-02-01 10:00:00"));
        p2.setUpdatedAt(p2.getCreatedAt());

        paymentRepo.saveAll(List.of(p1, p2));
    }

    @Test
    @DisplayName("Should find payments by date range")
    void shouldFindByDateRange() {
        Timestamp from = Timestamp.valueOf("2023-12-31 23:59:59");
        Timestamp to = Timestamp.valueOf("2024-01-15 00:00:00");

        Page<Payment> result = paymentRepo.findAllByFilter(
                from, to, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should find payments by Status and InquiryRefId")
    void shouldFindByStatusAndInquiry() {
        Page<Payment> result = paymentRepo.findAllByFilter(
                null, null, null, testInquiryId, PaymentStatus.PENDING, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInquiryRefId()).isEqualTo(testInquiryId);
    }

    @Test
    @DisplayName("Should return all payments when all filters are null")
    void shouldReturnAllWhenFiltersAreNull() {
        Page<Payment> result = paymentRepo.findAllByFilter(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

}