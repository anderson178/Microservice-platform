package com.iprody.inventory.kafka.config;

import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.InventoryRequest;
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

    private Map<String, Object> commonConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60_000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 45_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        return props;
    }

    @Bean("cancellationConsumerFactory")
    public ConsumerFactory<String, CancellationRequest> cancellationConsumerFactory() {
        Map<String, Object> configProps = commonConsumerConfigs();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-cancellation-group");
        JacksonJsonDeserializer<CancellationRequest> jsonDeserializer = new JacksonJsonDeserializer<>(CancellationRequest.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean("cancellationListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, CancellationRequest> cancellationListenerContainerFactory(
            ConsumerFactory<String, CancellationRequest> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, CancellationRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean("inventoryAvailabilityRequestConsumerFactory")
    public ConsumerFactory<String, InventoryRequest> inventoryAvailabilityRequestConsumerFactory() {
        Map<String, Object> configProps = commonConsumerConfigs();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-availability-request-group");
        JacksonJsonDeserializer<InventoryRequest> jsonDeserializer = new JacksonJsonDeserializer<>(InventoryRequest.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean("inventoryRequestListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, InventoryRequest> inventoryRequestListenerContainerFactory(
            ConsumerFactory<String, InventoryRequest> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, InventoryRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
