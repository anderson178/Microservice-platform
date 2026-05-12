package com.iprody.inquiry.kafka.event;

import com.iprody.common.kafka.CancellationResponse;
import com.iprody.common.kafka.KafkaEventTopic;
import com.iprody.inquiry.service.EventProcessorService;
import com.iprody.inquiry.service.ValidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CancellationResponseListener {
    private static final String TOPIC = KafkaEventTopic.CANCELLATION_RESPONSE;
    private static final String CONTAINER_FACTORY = "cancellationListenerContainerFactory";

    private final EventProcessorService eventProcessorService;
    private final ValidateService validator;

    @KafkaListener(topics = TOPIC, containerFactory = CONTAINER_FACTORY)
    public void consume(ConsumerRecord<String, CancellationResponse> record, Acknowledgment ack) {
        log.debug("Received cancellation response with key={}", record.key());
        log.debug("Received cancellation response from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        CancellationResponse event = record.value();
        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        try {
            eventProcessorService.processCancellationResponse(event);
            ack.acknowledge();
            log.info("Response processed cancelled successfully for event={}", event);
        } catch (Exception e) {
            log.error("Failed to process cancellation response for inquiryId={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
