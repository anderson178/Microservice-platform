package com.iprody.inquiry.kafka.event;

import com.iprody.common.ResultCode;
import com.iprody.common.exception.AppException;
import com.iprody.common.kafka.InventoryResponse;
import com.iprody.common.kafka.KafkaEventTopic;
import com.iprody.inquiry.service.EventProcessorService;
import com.iprody.inquiry.service.ValidateService;
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
public class InventoryResponseListener {
    private static final String TOPIC = KafkaEventTopic.INVENTORY_RESPONSE;
    private static final String CONTAINER_FACTORY = "inventoryResponseListenerContainerFactory";

    private final EventProcessorService eventProcessorService;
    private final ValidateService validator;

    @KafkaListener(topics = TOPIC, containerFactory = CONTAINER_FACTORY)
    public void consume(ConsumerRecord<String, InventoryResponse> record, Acknowledgment ack) {
        log.info("Received inventory response with key={}", record.key());
        log.debug("Received inventory response from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        InventoryResponse event = record.value();
        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        try {
            eventProcessorService.processInventoryResponse(UUID.fromString(record.key()), event);
            ack.acknowledge();
            log.info("Inventory response processed successfully for event={}", event);
        } catch (AppException e) {
            if (ResultCode.NOT_FOUND.equals(e.getCode())) {
                log.info("Response processed inventory error {} for inquiryRefID={}", e.getCode(), record.value());
                ack.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process inventory response for inquiryId={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
