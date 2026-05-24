package com.iprody.inquiry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class HTTPCustomerService {
    @Value("#{'${app.customer-service.url}' + '/api/v1/customers'}")
    private String url;

    private final RestTemplate restTemplate;


    public ResponseEntity<String> getById(UUID customerId, String tokenAuthentication) {
        return execute(() -> restTemplate.exchange(
                url + "/" + customerId,
                HttpMethod.GET,
                new HttpEntity<>(getHeaders(tokenAuthentication)),
                String.class));
    }

    private ResponseEntity<String> execute(Supplier<ResponseEntity<String>> request) {
        try {
            ResponseEntity<String> response = request.get();
            log.info("Response: status={}, body={}", response.getStatusCode(), response.getBody());
            return response;
        } catch (HttpStatusCodeException e) {
            log.error("Http error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Http connection error: {}", e.getMessage());
            throw e;
        }
    }

    private HttpHeaders getHeaders(String tokenAuthentication) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAuthentication);
        return headers;
    }
}
