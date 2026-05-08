package com.iprody.payment.utils;

import com.iprody.common.utils.UUIDUtils;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public final class ValidateService {
    private final Validator validator;

    public <T> boolean validate(ConsumerRecord<String, T> record) {
        if (!UUIDUtils.isUUID(record.key())) {
            log.info("Received unsupported key={}", record.key());
            return false;
        }

        return validator.validate(record.value()).stream()
                .findFirst()
                .map(v -> {
                    log.info("Validation failed for key {}: {} {}", record.key(), v.getPropertyPath(), v.getMessage());
                    return false;
                })
                .orElse(true);
    }
}
