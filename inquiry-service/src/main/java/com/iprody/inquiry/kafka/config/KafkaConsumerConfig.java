package com.iprody.inquiry.kafka.config;

import com.iprody.common.kafka.CancellationResponse;
import com.iprody.common.kafka.InventoryResponse;
import com.iprody.common.kafka.PaymentResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;


@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean("cancellationConsumerFactory")
    public ConsumerFactory<String, CancellationResponse> cancellationConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "inquiry-cancellation-group");
        JacksonJsonDeserializer<CancellationResponse> jsonDeserializer = new JacksonJsonDeserializer<>(CancellationResponse.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean("cancellationListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, CancellationResponse> cancellationListenerContainerFactory(
            ConsumerFactory<String, CancellationResponse> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, CancellationResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean("paymentResponseConsumerFactory")
    public ConsumerFactory<String, PaymentResponse> paymentResponseConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-inquiry-group");
        JacksonJsonDeserializer<PaymentResponse> jsonDeserializer = new JacksonJsonDeserializer<>(PaymentResponse.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean("paymentResponseListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, PaymentResponse> paymentResponseListenerContainerFactory(
            ConsumerFactory<String, PaymentResponse> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, PaymentResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean("inventoryResponseConsumerFactory")
    public ConsumerFactory<String, InventoryResponse> inventoryResponseConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-inquiry-group");
        JacksonJsonDeserializer<InventoryResponse> jsonDeserializer = new JacksonJsonDeserializer<>(InventoryResponse.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean("inventoryResponseListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, InventoryResponse> inventoryResponseListenerContainerFactory(
            ConsumerFactory<String, InventoryResponse> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, InventoryResponse> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
