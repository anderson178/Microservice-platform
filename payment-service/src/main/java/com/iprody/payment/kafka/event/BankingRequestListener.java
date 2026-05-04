package com.iprody.payment.kafka.event;

import com.iprody.common.kafka.KafkaEventRout;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
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

        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        BankRequest event = record.value();
        UUID inquiryRefId = event.getInquiryRefId();

        if (paymentService.isAlreadyProcessedByBank(inquiryRefId)) {
            log.info("Payment with inquiryRefId={} already sent. Skipping.", inquiryRefId);
            ack.acknowledge();
            return;
        }

        processBankInteraction(event, ack);
    }

    private void processBankInteraction(BankRequest event, Acknowledgment ack) {
        try {
            ResponseEntity<String> response = httpClientService.sendBankRequest(event);
            validateHttpResponse(response);

            BankResponse bankResponse = parseBankResponse(response.getBody());
            handleBankResponseStatus(event.getInquiryRefId(), bankResponse);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error calling banking service for inquiryRefId={}", event.getInquiryRefId(), e);
            throw e;
        }
    }

    private void validateHttpResponse(ResponseEntity<String> response) {
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Bank API unavailable, triggering retry...");
        }
    }

    private BankResponse parseBankResponse(String body) {
        if (StringUtils.isBlank(body)) {
            throw new IllegalArgumentException("Empty response body from bank");
        }

        BankResponse bankResponse = JsonStructUtils.fromJsonSafe(BankResponse.class, body);
        if (bankResponse == null) {
            log.error("Failed to parse bank response body: {}", body);
            throw new IllegalArgumentException("Invalid JSON format from bank");
        }
        return bankResponse;
    }

    private void handleBankResponseStatus(UUID inquiryRefId, BankResponse bankResponse) {
        if (BankResponse.Status.PROCESSING.equals(bankResponse.getStatus())) {
            eventProcessorService.responseBankInitialStageProcessing(inquiryRefId, bankResponse);
        } else {
            eventProcessorService.responseBankProcessing(inquiryRefId, bankResponse);
        }
    }
}
