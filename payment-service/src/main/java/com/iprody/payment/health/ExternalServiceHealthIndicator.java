package com.iprody.payment.health;

import com.iprody.payment.service.HTTPClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.UUID;


@RequiredArgsConstructor
@Component("payment-provider")
public class ExternalServiceHealthIndicator implements HealthIndicator {
    private final HTTPClientService httpClientService;

    @Override
    public Health health() {
        try {
            httpClientService.getPaymentStatus(UUID.randomUUID());
            return Health.up()
                    .withDetail("status", "Connected")
                    .build();

        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().is4xxClientError()) {
                return Health.up()
                        .withDetail("status", "Connected")
                        .withDetail("response", e.getStatusCode().toString())
                        .build();
            }

            return Health.status("OUT_OF_SERVICE")
                    .withDetail("error", "The provider returned a server error:" + e.getStatusCode())
                    .build();

        } catch (Exception e) {
            return Health.status("OUT_OF_SERVICE")
                    .withDetail("error", "The provider is unavailable over the network:" + e.getMessage())
                    .build();
        }
    }
}
