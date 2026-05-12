package com.iprody.inquiry.kafka.event;

import com.iprody.common.kafka.InventoryRequest;
import com.iprody.common.kafka.KafkaEventTopic;
import com.iprody.common.kafka.PaymentResponse;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.inquiry.configuration.KafkaTestConfig;
import com.iprody.inquiry.configuration.PostgresTestConfig;
import com.iprody.inquiry.model.*;
import com.iprody.inquiry.repository.InquiryRepo;
import com.iprody.inquiry.repository.OutboxEventRepo;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Import({PostgresTestConfig.class, KafkaTestConfig.class})
@DisplayName("Payment response Integration Test")
class PaymentResponseListenerIT {
    private static final String PAYMENT_RESPONSE_TOPIC = KafkaEventTopic.PAYMENT_RESPONSE;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected InquiryRepo inquiryRep;

    @Autowired
    protected OutboxEventRepo outboxEventRepo;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepo.deleteAll();
        inquiryRep.deleteAll();
    }

    @Test
    @DisplayName("APPROVED payment -> inquiry status=PAID + inventory.request sent")
    void shouldProcessApprovedPaymentAndSendInventoryRequest() throws Exception {
        UUID inquiryId = saveInquiry(InquiryStatus.PAYMENT, 2L).getId();

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.APPROVED);
        response.setInquiryRefId(inquiryId);
        response.setCurrency("USD");
        response.setAmount(new BigDecimal("100.00"));

        kafkaTemplate.send(PAYMENT_RESPONSE_TOPIC, inquiryId.toString(), response);
        kafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inquiry updated = inquiryRep.findById(inquiryId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(InquiryStatus.PAID);
                });

        await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                            inquiryId, OutboxEventType.INVENTORY_REQUEST);
                    assertThat(events).hasSize(1);
                    assertThat(events.get(0).getStatus()).isEqualTo(OutboxEventStatus.PENDING);
                });

        outboxPublisher.publishPendingEvents();

        ConsumerRecord<String, InventoryRequest> received = consumeInventoryRequest(inquiryId);
        assertThat(received).isNotNull();
        assertThat(received.value().getInquiryRefId()).isEqualTo(inquiryId);
        assertThat(received.value().getNumberOfSeats()).isEqualTo(2L);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OutboxEvent published = outboxEventRepo.findById(
                                    outboxEventRepo.findByAggregateIdAndEventType(
                                            inquiryId, OutboxEventType.INVENTORY_REQUEST).get(0).getId())
                            .orElseThrow();
                    assertThat(published.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
                });
    }

    @Test
    @DisplayName("DECLINED payment -> inquiry status=CANCELLED, no inventory request")
    void shouldCancelInquiryOnDeclinedPayment() {
        UUID inquiryId = saveInquiry(InquiryStatus.PAYMENT, 1L).getId();

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.DECLINED);
        response.setInquiryRefId(inquiryId);
        response.setCurrency("USD");
        response.setAmount(new BigDecimal("100.00"));
        response.setReason("Insufficient funds");

        kafkaTemplate.send(PAYMENT_RESPONSE_TOPIC, inquiryId.toString(), response);
        kafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inquiry updated = inquiryRep.findById(inquiryId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(InquiryStatus.CANCELLED);
                    assertThat(updated.getNote()).contains("Insufficient funds");
                });

        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                inquiryId, OutboxEventType.INVENTORY_REQUEST);
        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("Invalid message (null key) → acknowledged but not processed")
    void shouldAcknowledgeInvalidMessage() throws Exception {
        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.APPROVED);

        kafkaTemplate.send(PAYMENT_RESPONSE_TOPIC, null, response).get();

        Thread.sleep(2000);

        assertThat(outboxEventRepo.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("RECEIVED payment -> inquiry status=PAYMENT, no inventory request")
    void shouldProcessPendingPaymentWithoutInventoryRequest() {
        UUID inquiryId = saveInquiry(InquiryStatus.NEW, 1L).getId();

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.RECEIVED);
        response.setInquiryRefId(inquiryId);
        response.setCurrency("USD");
        response.setAmount(new BigDecimal("100.00"));

        kafkaTemplate.send(PAYMENT_RESPONSE_TOPIC, inquiryId.toString(), response);
        kafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inquiry updated = inquiryRep.findById(inquiryId).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(InquiryStatus.PAYMENT);
                });

        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                inquiryId, OutboxEventType.INVENTORY_REQUEST);
        assertThat(events).isEmpty();
    }

    @Test
    @DisplayName("Non-existent inquiryId -> error logged, message acknowledged")
    void shouldHandleNonExistentInquiryId() throws Exception {
        UUID fakeId = UUID.randomUUID();

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.APPROVED);

        kafkaTemplate.send(PAYMENT_RESPONSE_TOPIC, fakeId.toString(), response).get();

        Thread.sleep(2000);

        List<OutboxEvent> events = outboxEventRepo.findByAggregateIdAndEventType(
                fakeId, OutboxEventType.INVENTORY_REQUEST);
        assertThat(events).isEmpty();
    }

    private Inquiry saveInquiry(InquiryStatus status, Long seats) {
        Inquiry inquiry = new Inquiry();
        inquiry.setGroupRefId(UUID.randomUUID());
        inquiry.setCustomerRefId(UUID.randomUUID());
        inquiry.setManagerRefId(UUID.randomUUID());
        inquiry.setNumberOfSeats(seats);
        inquiry.setStatus(status);
        inquiry.setSource("WEB");
        inquiry.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        inquiryRep.save(inquiry);
        inquiryRep.flush();

        return inquiry;
    }

    private ConsumerRecord<String, InventoryRequest> consumeInventoryRequest(UUID inquiryId) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaTestConfig.KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumerProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.iprody.common.kafka");
        consumerProps.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, InventoryRequest.class);
        consumerProps.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        ConsumerFactory<String, InventoryRequest> cf =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps,
                        new StringDeserializer(),
                        new JacksonJsonDeserializer<>(InventoryRequest.class, false)
                );

        Consumer<String, InventoryRequest> consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList("inventory.request"));

        ConsumerRecord<String, InventoryRequest> record = null;
        Duration timeout = Duration.ofSeconds(3);
        Instant end = Instant.now().plus(timeout);

        while (Instant.now().isBefore(end) && record == null) {
            var records = consumer.poll(Duration.ofMillis(100));
            for (var r : records) {
                if (r.value().getInquiryRefId().equals(inquiryId)) {
                    record = r;
                    break;
                }
            }
        }
        consumer.close();
        return record;
    }
}