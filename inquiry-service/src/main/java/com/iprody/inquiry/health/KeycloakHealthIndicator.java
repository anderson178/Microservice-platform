package com.iprody.inquiry.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component("keycloak")
public class KeycloakHealthIndicator implements HealthIndicator {
    private final RestClient restClient;
    private final String keycloakRealmUrl;
    private final String expectedIssuer;

    public KeycloakHealthIndicator(@Value("${app.keycloak.url}") String keycloakUrl) {
        keycloakRealmUrl = keycloakUrl + "/realms/iprody/.well-known/openid-configuration";
        expectedIssuer = keycloakUrl + "/realms/iprody";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(2000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(keycloakRealmUrl)
                .build();
    }

    @Override
    public Health health() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = restClient.get()
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(Map.class);

            if (responseBody != null && responseBody.containsKey("issuer")) {
                String issuer = String.valueOf(responseBody.get("issuer"));
                if (expectedIssuer.equalsIgnoreCase(issuer)) {
                    return Health.up()
                            .withDetail("realm", "iprody")
                            .withDetail("status", "Verified")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("realm", "iprody")
                            .withDetail("error", "Keycloak did not return the iprody realm configuration" + issuer)
                            .build();
                }
            }

            return Health.down()
                    .withDetail("realm", "iprody")
                    .withDetail("error", "Keycloak response does not contain 'issuer' field")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("realm", "iprody")
                    .withDetail("url", keycloakRealmUrl)
                    .withDetail("error", "Keycloak is unavailable or the realm is missing:" + e.getMessage())
                    .build();
        }
    }
}
