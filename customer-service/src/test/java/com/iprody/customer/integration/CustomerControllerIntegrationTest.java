package com.iprody.customer.integration;

import com.iprody.common.ResultCode;
import com.iprody.customer.configuration.ConfigurationTest;
import com.iprody.customer.configuration.PostgresTestConfig;
import com.iprody.customer.dto.ContractDataDto;
import com.iprody.customer.dto.CustomerDataDto;
import com.iprody.common.dto.CustomerDto;
import com.iprody.customer.model.Customer;
import com.iprody.customer.repository.ContractRepo;
import com.iprody.customer.repository.CustomerRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@EnableMethodSecurity
@Testcontainers
@ActiveProfiles("test")
@Sql(scripts = {"/sql/init-schema.sql"})
@Import({ConfigurationTest.class, PostgresTestConfig.class})
@DisplayName("Customer Integration Tests (HTTP -> Service -> Repo -> DB)")
public class CustomerControllerIntegrationTest {
    @Autowired
    private WebApplicationContext context;

    private WebTestClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private ContractRepo contractRepo;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        customerRepo.deleteAll();
        contractRepo.deleteAll();

        Jwt mockJwt = Jwt.withTokenValue("mock-integration-token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "MANAGER")))
                .claim("preferred_username", "test-user")
                .build();

        org.mockito.Mockito.when(jwtDecoder.decode(org.mockito.Mockito.anyString())).thenReturn(mockJwt);

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
    @DisplayName("POST /")
    class CreateTests {

        @Test
        @DisplayName("should create customer, return 200, and persist to DB")
        void create_success_andVerifyInDb() {
            // Создание требует роль ADMIN
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
                    .headers(withFakeHeader())
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
                    .headers(withFakeHeader())
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
                    .headers(withFakeHeader())
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
                    .headers(withFakeHeader())
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
                    .headers(withFakeHeader())
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
                .headers(withFakeHeader())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .returnResult()
                .getResponseBody();
    }
}
