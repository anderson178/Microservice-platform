package com.iprody.payment.kafka.event;

import com.iprody.common.kafka.KafkaEventRout;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.common.utils.JsonStructUtils;
import com.iprody.payment.kafka.PaymentStillProcessingException;
import com.iprody.payment.kafka.dto.BankResponse;
import com.iprody.payment.service.EventProcessorService;
import com.iprody.payment.service.HTTPClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankingRequestCheckStatusListener {
    private static final String TOPIC = KafkaEventRout.BANKING_RESPONSE;
    private static final String GROUP = "payment-banking-request-check-status-group";
    private static final String CONTAINER_FACTORY = "bankingResponseListenerContainerFactory";

    private final HTTPClientService httpClientService;
    private final EventProcessorService eventProcessorService;


    @RetryableTopic(
            attempts = "5",
            backOff = @BackOff(delay = 10_000, multiplier = 2.0),
            include = {
                    PaymentStillProcessingException.class,
                    RuntimeException.class
            },
            retryTopicSuffix = "-status-retry",
            kafkaTemplate = "kafkaTemplate",
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = TOPIC, groupId = GROUP, containerFactory = CONTAINER_FACTORY)
    public void consume(ConsumerRecord<String, BankResponse> record, Acknowledgment ack) {
        UUID inquiryId = UUID.fromString(record.key());
        log.info("Checking status for inquiryRefId {}", inquiryId);

        UUID transactionId = record.value().getId();
        ResponseEntity<String> response = httpClientService.getPaymentStatus(transactionId);
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Bank API unavailable, triggering retry...");
        }

        if (StringUtils.isNotBlank(response.getBody())) {
            BankResponse bankResponse = JsonStructUtils.fromJsonSafe(BankResponse.class, response.getBody());
            if (bankResponse != null) {

                if (BankResponse.Status.PROCESSING.equals(bankResponse.getStatus())) {
                    log.info("Payment transactionId {} still in progress, triggering delayed retry", transactionId);
                    throw new PaymentStillProcessingException("Transaction " + bankResponse.getId() + " is not ready");
                }

                try {
                    eventProcessorService.responseBankProcessing(inquiryId, record.value());
                    ack.acknowledge();
                } catch (Exception e) {
                    log.error("Something went wrong", e);
                    throw e;
                }
            } else {
                log.error("Failed to parse bank response body");
                throw new IllegalArgumentException("Invalid JSON format from bank");
            }
        }
    }

    // A handler for "suicide bombers" who failed the retreats
    // ack.acknowledge() - automatically
    @DltHandler
    public void handleDlt(BankResponse event,
                          @Header(KafkaHeaders.RECEIVED_KEY) String key,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        UUID inquiryId = UUID.fromString(key);
        try {
            log.error("Event with key {} failed all retries in topic {}", key, topic);
            eventProcessorService.bankErrorHandle(inquiryId, PaymentStatus.REJECTED, event);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to execute DLT handler with key{} and topic {}", inquiryId, topic, e);
        }
    }
}
