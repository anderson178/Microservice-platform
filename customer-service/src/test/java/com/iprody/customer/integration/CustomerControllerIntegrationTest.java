package com.iprody.customer.integration;

import com.iprody.common.ResultCode;
import com.iprody.customer.configuration.ConfigurationTest;
import com.iprody.customer.dto.ContractDataDto;
import com.iprody.customer.dto.CustomerDataDto;
import com.iprody.customer.dto.CustomerDto;
import com.iprody.customer.model.Customer;
import com.iprody.customer.repository.ContractRepo;
import com.iprody.customer.repository.CustomerRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureWebTestClient
@Testcontainers
@Sql(scripts = {"/sql/init-schema.sql"})
@Import(ConfigurationTest.class)
@DisplayName("Customer Integration Tests (HTTP → Service → Repo → DB)")
public class CustomerControllerIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("sql/init-schema.sql");

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private ContractRepo contractRepo;

    @BeforeEach
    void setUp() {
        customerRepo.deleteAll();
        contractRepo.deleteAll();
    }

    @Nested
    @DisplayName("POST /")
    class CreateTests {

        @Test
        @DisplayName("should create customer, return 200, and persist to DB")
        void create_success_andVerifyInDb() {
            CustomerDto responseCustomerDto = createCustomerViaHttp(
                    "Ivan",
                    "test@123.com",
                    "+79991112233");

            assertThat(responseCustomerDto).isNotNull();
            assertThat(responseCustomerDto.getContract()).isNotNull();
            assertThat(responseCustomerDto.getContract().getId()).isNotNull();
            assertThat(responseCustomerDto.getId()).isNotNull();

            Customer dbCustomer = customerRepo.findById(responseCustomerDto.getId()).orElseThrow();

            assertThat(dbCustomer.getFullName()).isEqualTo(responseCustomerDto.getFullName());
            assertThat(dbCustomer.getContract().getEmail()).isEqualTo(responseCustomerDto.getContract().getEmail());
            assertThat(dbCustomer.getContract().getPhoneNumber()).isEqualTo(responseCustomerDto.getContract().getPhoneNumber());
            assertThat(dbCustomer.getCreatedAt()).isNotNull();
            assertThat(dbCustomer.getContract().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return 404 when customer not found")
        void shouldReturn404() {
            UUID nonExistentId = UUID.randomUUID();

            webClient.get()
                    .uri("/api/v1/customers/{id}", nonExistentId)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResultCode.NOT_FOUND.name());
        }

        @Test
        @DisplayName("should return 400 when valid fails")
        void create_validationError() {
            CustomerDataDto invalidDto = new CustomerDataDto(null);
            invalidDto.setContract(new ContractDataDto("val@test.com", "123"));

            webClient.post()
                    .uri("/api/v1/customers")
                    .bodyValue(invalidDto)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResultCode.VALIDATION_ERROR.name())
                    .jsonPath("$.message").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("GET /")
    class SearchTests {

        @Test
        @DisplayName("should filter case-insensitively and sort correctly")
        void search_filtersAndSorts() {
            createCustomerViaHttp("Alice Smith");
            createCustomerViaHttp("bob jones");
            createCustomerViaHttp("ALICE Wonder");

            webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/customers/search")
                            .queryParam("filter.fullName", "alice")
                            .queryParam("pagination.offset", "0")
                            .queryParam("pagination.limit", "10")
                            .queryParam("sorting.sortField", "FULL_NAME")
                            .queryParam("sorting.sortDirection", "ASC")
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.size()").isEqualTo("2")
                    .jsonPath("$.data[0].fullName").isEqualTo("ALICE Wonder")
                    .jsonPath("$.data[1].fullName").isEqualTo("Alice Smith")
                    .jsonPath("$.totalCount").isEqualTo("2");
        }
    }

    @Nested
    @DisplayName("PUT /")
    class UpdateTests {

        @Test
        @DisplayName("should update customer & contract, verify DB changes")
        void update_success_andVerifyInDb() {
            UUID initialId = createCustomerViaHttp("Old Name", "old@mail.com", "000").getId();

            CustomerDataDto updateDto = new CustomerDataDto("New Name");
            updateDto.setContract(new ContractDataDto("new@mail.com", "999"));

            webClient.put()
                    .uri("/api/v1/customers/{id}", initialId)
                    .bodyValue(updateDto)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody();

            Customer updated = customerRepo.findById(initialId).orElseThrow();
            assertThat(updated.getFullName()).isEqualTo("New Name");
            assertThat(updated.getContract().getEmail()).isEqualTo("new@mail.com");
            assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
        }

        @Test
        @DisplayName("should return 404 when updating non-existent ID")
        void update_notFound() {
            CustomerDataDto dto = new CustomerDataDto("Test");
            dto.setContract(new ContractDataDto("x@y.z", "123"));

            webClient.put()
                    .uri("/api/v1/customers/{id}", UUID.randomUUID())
                    .bodyValue(dto)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(ResultCode.NOT_FOUND.name());
        }
    }

    private void createCustomerViaHttp(String fullName) {
        createCustomerViaHttp(fullName, "", "");
    }

    private CustomerDto createCustomerViaHttp(String fullName, String email, String phone) {
        CustomerDataDto customerDataDto = new CustomerDataDto(fullName);
        customerDataDto.setContract(new ContractDataDto(email, phone));

        return webClient.post()
                .uri("/api/v1/customers")
                .bodyValue(customerDataDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .returnResult()
                .getResponseBody();
    }
}
