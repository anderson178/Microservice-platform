package com.iprody.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class HTTPClientService {
    @Value("${app.payment-provider.url}")
    private String urlPaymentProvider;

    @Value("${app.payment-provider.credentials.user}")
    private String user;

    @Value("${app.payment-provider.credentials.pwd}")
    private String pwd;

    private final RestTemplate restTemplate;


    public ResponseEntity<String> sendBankRequest(Object body) {
        return execute(() -> restTemplate.exchange(
                urlPaymentProvider,
                HttpMethod.POST,
                new HttpEntity<>(
                        body,
                        getHeaders()),
                String.class));
    }

    public ResponseEntity<String> getPaymentStatus(UUID transactionId) {
        return execute(() -> restTemplate.exchange(
                urlPaymentProvider + "/" + transactionId,
                HttpMethod.GET,
                new HttpEntity<>(getHeaders()),
                String.class));
    }

    private ResponseEntity<String> execute(Supplier<ResponseEntity<String>> request) {
        try {
            ResponseEntity<String> response = request.get();
            log.info("Bank response: status={}, body={}", response.getStatusCode(), response.getBody());
            return response;
        } catch (HttpStatusCodeException e) {
            log.error("Bank error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Bank connection error: {}", e.getMessage());
            throw e;
        }
    }

    private HttpHeaders getHeaders() {
        String auth = user + ":" + pwd;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        headers.set("X-Pay-Account", "paymentAgentIprodyApiToken");
        return headers;
    }
}
