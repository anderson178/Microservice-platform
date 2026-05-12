package com.iprody.inquiry.kafka.event;

import com.iprody.common.kafka.KafkaEventTopic;
import com.iprody.common.kafka.PaymentResponse;
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
public class PaymentResponseListener {
    private static final String TOPIC = KafkaEventTopic.PAYMENT_RESPONSE;
    private static final String CONTAINER_FACTORY = "paymentResponseListenerContainerFactory";

    private final EventProcessorService eventProcessorService;
    private final ValidateService validator;

    @KafkaListener(topics = TOPIC, containerFactory = CONTAINER_FACTORY)
    public void consume(ConsumerRecord<String, PaymentResponse> record, Acknowledgment ack) {
        log.info("Received payment response with key={}", record.key());
        log.debug("Received payment response from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        PaymentResponse event = record.value();
        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        try {
            eventProcessorService.processPaymentResponse(event);
            ack.acknowledge();
            log.info("Response processed cancelled successfully for event={}", event);
        } catch (Exception e) {
            log.error("Failed to process cancellation response for inquiryId={}: {}", record.key(), e.getMessage(), e);
        }
    }
}
