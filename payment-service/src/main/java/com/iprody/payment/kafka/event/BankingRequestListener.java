package com.iprody.payment.kafka.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iprody.common.kafka.KafkaEventRout;
import com.iprody.common.struct.PaymentStatus;
import com.iprody.common.utils.JsonStructUtils;
import com.iprody.payment.kafka.dto.BankRequest;
import com.iprody.payment.kafka.dto.BankResponse;
import com.iprody.payment.service.EventProcessorService;
import com.iprody.payment.service.HTTPClientService;
import com.iprody.payment.service.PaymentService;
import com.iprody.payment.utils.ValidateService;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankingRequestListener {
    private static final String TOPIC = KafkaEventRout.BANKING_REQUEST;
    private static final String GROUP = "payment-banking-request-group";
    private static final String CONTAINER_FACTORY = "bankingRequestListenerContainerFactory";

    private final ValidateService validator;
    private final PaymentService paymentService;
    private final HTTPClientService httpClientService;
    private final EventProcessorService eventProcessorService;


    @RetryableTopic(
            attempts = "5",
            backOff = @BackOff(delay = 1_000, multiplier = 2.0),
            include = {
                    RuntimeException.class,
                    ResourceAccessException.class,
                    org.springframework.web.client.HttpServerErrorException.class
            },
            retryTopicSuffix = "-request-retry",
            kafkaTemplate = "kafkaTemplate",
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = TOPIC, groupId = GROUP, containerFactory = CONTAINER_FACTORY)
    public void consume(ConsumerRecord<String, BankRequest> record, Acknowledgment ack) {
        log.info("Received banking request with key={}", record.key());
        log.debug("Received banking request from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        BankRequest event = record.value();
        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        log.info("Processing http request to bankingService");

        try {
            UUID inquiryRefId = event.getInquiryRefId();
            if (paymentService.isAlreadyProcessedByBank(inquiryRefId)) {
                log.info("Payment with inquiryRefId={} was already sent to bank. Skipping HTTP call.", inquiryRefId);
                ack.acknowledge();
                return;
            }

            ResponseEntity<String> response = httpClientService.sendBankRequest(event);
            if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Bank API unavailable, triggering retry...");
            }

            if (StringUtils.isNotBlank(response.getBody())) {
                BankResponse bankResponse = JsonStructUtils.fromJsonSafe(BankResponse.class, response.getBody());
                if (bankResponse != null) {
                    if (BankResponse.Status.PROCESSING.equals(bankResponse.getStatus())) {
                        eventProcessorService.responseBankInitialStageProcessing(event.getInquiryRefId(), bankResponse);
                    } else {
                        eventProcessorService.responseBankProcessing(inquiryRefId, bankResponse);
                    }

                    ack.acknowledge();
                } else {
                    log.error("Failed to parse bank response body");
                    throw new IllegalArgumentException("Invalid JSON format from bank");
                }
            }
        } catch (Exception e) {
            log.error("Error calling banking service", e);
            throw e;
        }
    }

    // A handler for "suicide bombers" who failed the retreats
    // ack.acknowledge() - automatically
    @DltHandler
    public void handleDlt(BankRequest event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            log.error("Event {} failed all retries in topic {}", event.getInquiryRefId(), topic);
            eventProcessorService.bankErrorHandle(event.getInquiryRefId(), PaymentStatus.REJECTED, event);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to execute DLT handler with key{} and topic {}", event.getInquiryRefId(), topic, e);
        }
    }
}
