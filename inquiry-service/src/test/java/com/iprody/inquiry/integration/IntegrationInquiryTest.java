package com.iprody.inquiry.integration;

import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.common.dto.SortDirectionTypeDto;
import com.iprody.common.kafka.CancellationRequest;
import com.iprody.common.kafka.CancellationStatus;
import com.iprody.inquiry.configuration.KafkaTestConfig;
import com.iprody.inquiry.configuration.PostgresTestConfig;
import com.iprody.inquiry.dto.CancellationRequestDto;
import com.iprody.inquiry.dto.InquiryDataDto;
import com.iprody.inquiry.dto.InquiryDto;
import com.iprody.inquiry.dto.InquirySortFieldDto;
import com.iprody.inquiry.kafka.event.OutboxPublisher;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryStatus;
import com.iprody.inquiry.repository.InquiryRepo;
import com.iprody.inquiry.service.HTTPCustomerService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@EnableMethodSecurity
@Testcontainers
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, KafkaTestConfig.class})
@DisplayName("Inquiry Integration Tests (HTTP -> Service -> Repo -> DB)")
public class IntegrationInquiryTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private KafkaContainer kafka;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private InquiryRepo inquiryRepo;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private HTTPCustomerService httpCustomerService;

    private UUID customerRefId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        inquiryRepo.deleteAll();

        Jwt mockJwt = Jwt.withTokenValue("mock-integration-token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "MANAGER")))
                .claim("preferred_username", "test-user")
                .build();

        Mockito.when(jwtDecoder.decode(org.mockito.Mockito.anyString())).thenReturn(mockJwt);

        String validCustomerJson = """
                {
                    "id": "%s",
                    "fullName": "Test Customer Integration",
                    "createdAt": "2026-05-24T20:00:00"
                }
                """.formatted(customerRefId);
        ;

        ResponseEntity<String> mockResponse = ResponseEntity.ok(validCustomerJson);
        Mockito.when(httpCustomerService.getById(Mockito.any(), Mockito.anyString()))
                .thenReturn(mockResponse);

        this.webClient = MockMvcWebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    /**
     * IMPORTANT: Why do we use this method instead of the standard .with(jwt()) or .mutateWith(mockJwt()):
     * <p>
     * 1. Our project is written in Spring MVC (Servlet/Tomcat), but we use WebTestClient for tests.
     * 2. The reactive mutator `.mutateWith(mockJwt())` is architecturally intended ONLY for WebFlux applications.
     * In an MVC application, calling it results in a `NullPointerException` (httpHandlerBuilder is null).
     * 3. To avoid framework conflicts, we pass a plain text Bearer token header.
     * WebTestClient treats it as a standard string and does not fail with a compatibility error.
     * 4. The actual role checking is performed by the @MockitoBean JwtDecoder bean, which intercepts
     * this "mock-integration-token" string and inserts a ready-made Jwt object into the security context
     * with ADMIN/MANAGER permissions configured in the setUp() method.
     */
    private Consumer<HttpHeaders> withFakeHeader() {
        return headers -> {
            // We use a dummy string. The real authorization is intercepted in the filter.
            headers.setBearerAuth("mock-integration-token");
        };
    }

    @Nested
    @DisplayName("POST /inquires")
    class CreateTests {

        @Test
        @DisplayName("should create inquiry, return 200, and persist to DB")
        void create_success_andVerifyInDb() {
            InquiryDataDto requestDto = new InquiryDataDto();
            requestDto.setManagerRefId(UUID.randomUUID());
            requestDto.setCustomerRefId(customerRefId);
            requestDto.setGroupRefId(UUID.randomUUID());
            requestDto.setSource("web");
            requestDto.setNumberOfSeats(2L);

            InquiryDto responseDto = webClient.post()
                    .uri("/api/v1/inquires")
                    .headers(withFakeHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(InquiryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(responseDto).isNotNull();
            assert responseDto != null;
            assertThat(responseDto.getId()).isNotNull();
            assertThat(responseDto.getStatus()).isEqualTo(InquiryStatus.NEW);

            Inquiry dbInquiry = inquiryRepo.findById(responseDto.getId()).orElseThrow();
            assertThat(dbInquiry.getManagerRefId()).isEqualTo(requestDto.getManagerRefId());
            assertThat(dbInquiry.getStatus()).isEqualTo(InquiryStatus.NEW);
            assertThat(dbInquiry.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("POST /inquires - Negative cases")
    class CreateNegativeTests {
        @Test
        @DisplayName("should return 400 when managerRefId is null (validation)")
        void create_nullManagerRefId_returns400() {
            InquiryDataDto invalidDto = new InquiryDataDto();
            invalidDto.setManagerRefId(null);

            webClient.post()
                    .uri("/api/v1/inquires")
                    .headers(withFakeHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResultCode.VALIDATION_ERROR.name())
                    .jsonPath("$.message").isNotEmpty();
        }

        @Test
        @DisplayName("should return 400 when request body is empty")
        void create_emptyBody_returns400() {
            webClient.post()
                    .uri("/api/v1/inquires")
                    .headers(withFakeHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResultCode.BAD_REQUEST.name());
        }
    }

    @Nested
    @DisplayName("GET /inquires/search")
    class SearchTests {

        @Test
        @DisplayName("should filter by status, customerRefId, managerRefId and sort correctly")
        void search_filtersAndSorts() {
            createInquiryViaHttp(UUID.randomUUID());
            createInquiryViaHttp(UUID.randomUUID());
            createInquiryViaHttp(UUID.randomUUID());
            createInquiryViaHttp(UUID.randomUUID());

            ResultList<InquiryDto> result = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/inquires/search")
                            .queryParam("filter.status", InquiryStatus.NEW.name())
                            .queryParam("filter.customerRefId", customerRefId)
                            .queryParam("pagination.offset", "0")
                            .queryParam("pagination.limit", "10")
                            .queryParam("sorting.sortField", InquirySortFieldDto.CREATED_AT.name())
                            .queryParam("sorting.sortDirection", SortDirectionTypeDto.ASC.name())
                            .build())
                    .headers(withFakeHeader())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new org.springframework.core.ParameterizedTypeReference<ResultList<InquiryDto>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assert result != null;
            assertThat(result.getTotalCount()).isEqualTo(4L);
            assertThat(result.getData().get(0).getStatus()).isEqualTo(InquiryStatus.NEW);
            assertThat(result.getData().get(0).getCustomerRefId()).isEqualTo(customerRefId);
        }

        @Test
        @DisplayName("should return all inquiries when no filters provided")
        void search_noFilters_returnsAll() {
            createInquiryViaHttp(UUID.randomUUID());
            createInquiryViaHttp(UUID.randomUUID());

            ResultList<InquiryDto> result = webClient.get()
                    .uri("/api/v1/inquires/search")
                    .headers(withFakeHeader())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(new ParameterizedTypeReference<ResultList<InquiryDto>>() {
                    })
                    .returnResult()
                    .getResponseBody();

            assert result != null;
            assertThat(result.getTotalCount()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("GET /inquires/search - Negative cases")
    class SearchNegativeTests {
        @Test
        @DisplayName("should return 400 when pagination.limit is negative")
        void search_negativeLimit_returns400() {
            webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/inquires/search")
                            .queryParam("pagination.limit", "-1")
                            .queryParam("pagination.offset", "0")
                            .build())
                    .headers(withFakeHeader())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResultCode.PAGE_CONSTRAINTS_ARE_NOT_SATISFIED.name());
        }

        @Test
        @DisplayName("should return 400 when pagination.offset is negative")
        void search_negativeOffset_returns400() {
            webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/inquires/search")
                            .queryParam("pagination.limit", "10")
                            .queryParam("pagination.offset", "-5")
                            .build())
                    .headers(withFakeHeader())
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("should return 400 when sorting.sortDirection is invalid enum")
        void search_invalidSortDirection_returns400() {
            webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/inquires/search")
                            .queryParam("sorting.sortDirection", "INVALID_VALUE")
                            .build())
                    .headers(withFakeHeader())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isNotEmpty();
        }

        @Test
        @DisplayName("should return 400 when filter.customerRefId is invalid UUID")
        void search_invalidCustomerRefId_returns400() {
            webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/inquires/search")
                            .queryParam("filter.customerRefId", "not-a-uuid")
                            .build())
                    .headers(withFakeHeader())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("POST /inquires/cancel")
    class CancelTests {

        @Test
        @DisplayName("should accept valid cancellation request, return 202, and publish to Kafka")
        void cancel_validInquiry_publishesToKafka() {
            InquiryDataDto createDto = new InquiryDataDto();
            createDto.setGroupRefId(UUID.randomUUID());
            createDto.setCustomerRefId(customerRefId);
            createDto.setManagerRefId(UUID.randomUUID());
            createDto.setSource("WEB");
            createDto.setNumberOfSeats(2L);

            InquiryDto created = webClient.post()
                    .uri("/api/v1/inquires")
                    .headers(withFakeHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(InquiryDto.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(created).isNotNull();
            assert created != null;
            UUID inquiryId = created.getId();

            CancellationRequestDto cancelDto = new CancellationRequestDto();
            cancelDto.setId(inquiryId);
            cancelDto.setStatus(CancellationStatus.RECEIVED);
            cancelDto.setReason("Customer requested cancellation");

            webClient.post()
                    .uri("/api/v1/inquires/cancel")
                    .headers(withFakeHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cancelDto)
                    .exchange()
                    .expectStatus().isAccepted();

            outboxPublisher.publishPendingEvents();

            String bootstrapServers = kafka.getBootstrapServers();
            try (KafkaConsumer<String, CancellationRequest> consumer = createTestConsumer(bootstrapServers)) {
                consumer.subscribe(Collections.singletonList("cancellation.request"));

                var found = await()
                        .atMost(5, TimeUnit.SECONDS)
                        .pollInterval(100, TimeUnit.MILLISECONDS)
                        .until(() -> {
                            List<ConsumerRecord<String, CancellationRequest>> records = pollRecords(consumer, Duration.ofMillis(100));
                            return records.stream()
                                    .filter(record -> inquiryId.toString().equals(record.key()))
                                    .findFirst();
                        }, Optional::isPresent);

                assertThat(found.get().value().getId()).isEqualTo(inquiryId);
                assertThat(found).as("Message should be published to cancellation.request").isPresent();
                assertThat(found.get().value().getId()).isEqualTo(inquiryId);
                assertThat(found.get().value().getStatus()).isEqualTo(CancellationStatus.RECEIVED);
                assertThat(found.get().value().getReason()).isEqualTo("Customer requested cancellation");
            }
        }

        private List<ConsumerRecord<String, CancellationRequest>> pollRecords(KafkaConsumer<String, CancellationRequest> consumer, Duration timeout) {
            List<ConsumerRecord<String, CancellationRequest>> allRecords = new ArrayList<>();
            Instant endTime = Instant.now().plus(timeout);

            while (Instant.now().isBefore(endTime)) {
                ConsumerRecords<String, CancellationRequest> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, CancellationRequest> record : records) {
                    allRecords.add(record);
                }
            }
            return allRecords;
        }

        @Test
        @DisplayName("should return 404 when inquiry not found")
        void cancel_nonExistentInquiry_returns404() {
            CancellationRequestDto cancelDto = new CancellationRequestDto();
            cancelDto.setId(UUID.randomUUID());
            cancelDto.setStatus(CancellationStatus.RECEIVED);
            cancelDto.setReason("Test");

            webClient.post()
                    .uri("/api/v1/inquires/cancel")
                    .headers(withFakeHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cancelDto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResultCode.NOT_FOUND.name())
                    .jsonPath("$.message").isNotEmpty();
        }
    }

    private KafkaConsumer<String, CancellationRequest> createTestConsumer(String bootstrapServers) {
        java.util.Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);

        props.put("spring.json.trusted.packages", "com.iprody.inquiry.kafka");
        props.put("spring.json.value.default.type", CancellationRequest.class);
        props.put("spring.json.use.type.headers", false);

        return new KafkaConsumer<>(props);
    }

    private void createInquiryViaHttp(UUID managerId) {
        InquiryDataDto dto = new InquiryDataDto();
        dto.setGroupRefId(UUID.randomUUID());
        dto.setCustomerRefId(customerRefId);
        dto.setManagerRefId(managerId);
        dto.setSource("web");
        dto.setNumberOfSeats(2L);

        webClient.post()
                .uri("/api/v1/inquires")
                .headers(withFakeHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(InquiryDto.class)
                .returnResult();
    }
}
