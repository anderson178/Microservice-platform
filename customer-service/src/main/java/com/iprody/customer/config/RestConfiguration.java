package com.iprody.customer.config;

import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestConfiguration {
    @Bean
    public CurrentTraceContext.ScopeDecorator mdcScopeDecorator() {
        return MDCScopeDecorator.get();
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
