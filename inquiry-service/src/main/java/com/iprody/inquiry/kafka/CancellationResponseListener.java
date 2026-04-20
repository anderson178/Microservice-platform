package com.iprody.inquiry.kafka;

import com.iprody.inquiry.service.EventProcessorService;
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
    private static final String TOPIC = "cancellation.response";

    private final EventProcessorService eventProcessorService;

    @KafkaListener(
            topics = TOPIC,
            groupId = "inquiry-cancellation-groupf55e59ca-1d66-4259-b082-c4784f160b9f:{\"id\":\"f55e59ca-1d66-4259-b082-c4784f160b9f\",\"status\":\"SUCCESS\",\"reason\":\"Test cancellation\"}\n",
            containerFactory = "cancellationListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, CancellationResponse> record, Acknowledgment ack) {
        log.info("Received cancellation response with key={}", record.key());
        log.debug("Received cancellation response from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        try {
            CancellationResponse event = record.value();
            eventProcessorService.processCancellationResponse(event);
            ack.acknowledge();
            log.info("Response processed cancelled successfully for event={}", event);
        } catch (Exception e) {
            log.error("Failed to process cancellation response for inquiryId={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
