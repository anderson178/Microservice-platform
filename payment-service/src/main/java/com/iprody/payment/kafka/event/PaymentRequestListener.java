package com.iprody.payment.kafka.event;

import com.iprody.common.kafka.PaymentRequest;
import com.iprody.payment.service.EventProcessorService;
import com.iprody.payment.utils.ValidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestListener {
    private static final String TOPIC = "payment.request";

    private final EventProcessorService eventProcessorService;
    private final ValidateService validator;

    @KafkaListener(
            topics = TOPIC,
            groupId = "payment-inquiry-group",
            containerFactory = "paymentRequestListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, PaymentRequest> record, Acknowledgment ack) {
        log.info("Received payment request with key={}", record.key());
        log.debug("Received payment request from topic: {}, partition: {}, offset: {}",
                record.topic(), record.partition(), record.offset());

        PaymentRequest event = record.value();
        if (!validator.validate(record)) {
            ack.acknowledge();
            return;
        }

        try {
            eventProcessorService.handlePayment(event);
            ack.acknowledge();
            log.info("Response processed payment successfully for event={}", event);
        } catch (Exception e) {
            log.error("Failed to process payment request for inquiryRefId={}: {}", record.value().getInquiryRefId(), e.getMessage(), e);
        }
    }
}
