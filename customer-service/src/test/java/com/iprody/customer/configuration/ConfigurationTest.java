package com.iprody.customer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ConfigurationTest {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
