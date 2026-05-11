package com.iprody.inventory.kafka.event;

import com.iprody.common.ResultCode;
import com.iprody.common.exception.AppException;
import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.KafkaEventRout;
import com.iprody.inventory.service.EventProcessorService;
import com.iprody.inventory.service.ValidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationRequestListener {
    public static final String TOPIC = KafkaEventRout.CANCELLATION_REQUEST;

    private final EventProcessorService eventProcessorService;
    private final ValidateService validator;

    @KafkaListener(
            topics = TOPIC,
            groupId = "inventory-cancellation-group",
            containerFactory = "cancellationListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, CancellationRequest> record, Acknowledgment ack) {
        log.debug("Received cancellation request with key={}", record.key());
        log.debug("Received cancellation request from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        try {
            eventProcessorService.processCancellationRequest(UUID.fromString(record.key()), record.value());
            ack.acknowledge();
            log.info("Request processed cancelled successfully for event={}", record.value());
        } catch (AppException e) {
            if (ResultCode.NOT_FOUND.equals(e.getCode())) {
                log.info("Request processed inventory cancellation returned error {} for inquiryRefID={}", e.getCode(), record.value());
                ack.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process cancellation request for record group id={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
