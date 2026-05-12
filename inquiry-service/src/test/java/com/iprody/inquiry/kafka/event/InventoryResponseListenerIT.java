package com.iprody.inquiry.kafka.event;

import com.iprody.common.kafka.InventoryResponse;
import com.iprody.common.kafka.InventoryStatus;
import com.iprody.common.kafka.KafkaEventTopic;
import com.iprody.inquiry.configuration.KafkaTestConfig;
import com.iprody.inquiry.configuration.PostgresTestConfig;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryStatus;
import com.iprody.inquiry.repository.InquiryRepo;
import com.iprody.inquiry.repository.OutboxEventRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Import({PostgresTestConfig.class, KafkaTestConfig.class})
@DisplayName("Inventory response Integration Test")
class InventoryResponseListenerIT {
    private static final String INVENTORY_RESPONSE_TOPIC = KafkaEventTopic.INVENTORY_RESPONSE;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected InquiryRepo inquiryRep;

    @Autowired
    protected OutboxEventRepo outboxEventRepo;

    @BeforeEach
    void setUp() {
        outboxEventRepo.deleteAll();
        inquiryRep.deleteAll();
    }

    @Test
    @DisplayName("RESERVED inventory -> inquiry status=COMPLETED")
    void shouldCompleteInquiryOnReservedInventory() {
        UUID inquiryId = saveInquiry().getId();

        InventoryResponse response = new InventoryResponse();
        response.setInquiryRefId(inquiryId);
        response.setGroupRefId(UUID.randomUUID());
        response.setStatus(InventoryStatus.RESERVED);

        kafkaTemplate.send(INVENTORY_RESPONSE_TOPIC, inquiryId.toString(), response);
        kafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inquiry updated = inquiryRep.findById(inquiryId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
                });
    }

    @Test
    @DisplayName("ROLLBACK with 'Group Full' reason -> inquiry status=MANUAL_PROCESSING_REQUIRED")
    void shouldSetManualProcessingOnGroupFullRollback() {
        UUID inquiryId = saveInquiry().getId();

        InventoryResponse response = new InventoryResponse();
        response.setInquiryRefId(inquiryId);
        response.setGroupRefId(UUID.randomUUID());
        response.setStatus(InventoryStatus.ROLLBACK);
        response.setReason("Group Full");

        kafkaTemplate.send(INVENTORY_RESPONSE_TOPIC, inquiryId.toString(), response);
        kafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inquiry updated = inquiryRep.findById(inquiryId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(InquiryStatus.MANUAL_PROCESSING_REQUIRED);
                    assertThat(updated.getNote()).contains("Group Full");
                });
    }

    @Test
    @DisplayName("ROLLBACK with other reason -> inquiry status=REJECTED")
    void shouldRejectInquiryOnOtherRollbackReason() {
        UUID inquiryId = saveInquiry().getId();

        InventoryResponse response = new InventoryResponse();
        response.setInquiryRefId(inquiryId);
        response.setGroupRefId(UUID.randomUUID());
        response.setStatus(InventoryStatus.ROLLBACK);
        response.setReason("Payment timeout");

        kafkaTemplate.send(INVENTORY_RESPONSE_TOPIC, inquiryId.toString(), response);
        kafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inquiry updated = inquiryRep.findById(inquiryId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(InquiryStatus.REJECTED);
                    assertThat(updated.getNote()).isEqualTo("Payment timeout");
                });
    }

    @Test
    @DisplayName("ROLLBACK with null reason -> inquiry status=REJECTED")
    void shouldRejectInquiryOnNullRollbackReason() {
        UUID inquiryId = saveInquiry().getId();

        InventoryResponse response = new InventoryResponse();
        response.setInquiryRefId(inquiryId);
        response.setGroupRefId(UUID.randomUUID());
        response.setStatus(InventoryStatus.ROLLBACK);
        response.setReason(null);

        kafkaTemplate.send(INVENTORY_RESPONSE_TOPIC, inquiryId.toString(), response);
        kafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inquiry updated = inquiryRep.findById(inquiryId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(InquiryStatus.REJECTED);
                    assertThat(updated.getNote()).isNull();
                });
    }

    @Test
    @DisplayName("Invalid message (null key) → acknowledged but not processed")
    void shouldAcknowledgeInvalidMessage() throws Exception {
        InventoryResponse response = new InventoryResponse();
        response.setStatus(InventoryStatus.RESERVED);

        kafkaTemplate.send(INVENTORY_RESPONSE_TOPIC, null, response).get();

        Thread.sleep(2000);

        assertThat(outboxEventRepo.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Non-existent inquiryId -> error logged, message acknowledged")
    void shouldHandleNonExistentInquiryId() throws Exception {
        UUID fakeId = UUID.randomUUID();

        InventoryResponse response = new InventoryResponse();
        response.setStatus(InventoryStatus.RESERVED);

        kafkaTemplate.send(INVENTORY_RESPONSE_TOPIC, fakeId.toString(), response).get();

        Thread.sleep(2000);

        assertThat(inquiryRep.findById(fakeId)).isEmpty();
    }

    private Inquiry saveInquiry() {
        Inquiry inquiry = new Inquiry();
        inquiry.setGroupRefId(UUID.randomUUID());
        inquiry.setCustomerRefId(UUID.randomUUID());
        inquiry.setManagerRefId(UUID.randomUUID());
        inquiry.setNumberOfSeats(2L);
        inquiry.setStatus(InquiryStatus.PAID);
        inquiry.setSource("WEB");
        inquiry.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        inquiryRep.save(inquiry);
        inquiryRep.flush();

        return inquiry;
    }

}