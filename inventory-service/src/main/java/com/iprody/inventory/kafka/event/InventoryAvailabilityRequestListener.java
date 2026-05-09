package com.iprody.inventory.kafka.event;

import com.iprody.common.ResultCode;
import com.iprody.common.exception.AppException;
import com.iprody.common.kafka.InventoryAvailabilityRequest;
import com.iprody.common.kafka.KafkaEventRout;
import com.iprody.inventory.service.EventProcessorService;
import com.iprody.inventory.service.ValidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAvailabilityRequestListener {
    public static final String TOPIC = KafkaEventRout.INVENTORY_AVAILABILITY_REQUEST;
    public static final String CONTAINER_FACTORY = "inventoryAvailabilityRequestListenerContainerFactory";

    private final EventProcessorService eventProcessorService;
    private final ValidateService validator;

    @KafkaListener(topics = TOPIC, containerFactory = CONTAINER_FACTORY)
    public void consume(ConsumerRecord<String, InventoryAvailabilityRequest> record, Acknowledgment ack) {
        log.info("Received inventory availability request with key={}", record.key());
        log.info("Received inventory availability request from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        try {
            eventProcessorService.processCheckAvailSeats(record.value());
            ack.acknowledge();
            log.info("Request processed inventory availability request successfully for event={}", record.value());
        } catch (AppException e) {
            if (ResultCode.NOT_FOUND.equals(e.getCode())) {
                log.info("Request processed inventory availability returned error {} for inquiryRefID={}", e.getCode(), record.value());
                ack.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process inventory availability request for record group id={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
