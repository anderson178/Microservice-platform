package com.iprody.customer.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@TestConfiguration
public class ConfigurationTest {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
