package com.iprody.customer.controller;


import com.iprody.customer.KeyCloakRoles;
import com.iprody.customer.configuration.SecurityConfiguration;
import com.iprody.customer.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfiguration.class)
@DisplayName("🔐 Authorization tests")
class AuthorizationTests {
    private static final String BASE_URL = "/api/v1/customers";
    private static final UUID TEST_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private JwtDecoder jwtDecoder;


    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor withRoles(String... roles) {
        return jwt()
                .jwt(jwt -> jwt
                        .claim("realm_access", Map.of("roles", List.of(roles)))
                        .claim("preferred_username", "test-user")
                )
                .authorities(Stream.of(roles)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList())
                );
    }


    @Nested
    @DisplayName("GET /{id}")
    class GetByIdAuthTests {

        @Test
        @DisplayName("401 when no token provided")
        void getById_noToken_returns401() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/{id}", TEST_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 when role INTERN (not allowed)")
        void getById_internRole_returns403() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/{id}", TEST_ID)
                            .with(withRoles(KeyCloakRoles.INTERN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 when role ADMIN")
        void getById_adminRole_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/{id}", TEST_ID)
                            .with(withRoles(KeyCloakRoles.ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 when role MANAGER")
        void getById_managerRole_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/{id}", TEST_ID)
                            .with(withRoles(KeyCloakRoles.MANAGER)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 when multiple roles including ADMIN")
        void getById_multipleRoles_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/{id}", TEST_ID)
                            .with(withRoles(KeyCloakRoles.INTERN, KeyCloakRoles.ADMIN)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST / - only ADMIN")
    class SaveAuthTests {

        @Test
        @DisplayName("401 when no token provided")
        void save_noToken_returns401() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test User\",\"email\":\"test@example.com\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 when role MANAGER (not allowed)")
        void save_managerRole_returns403() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test\"}")
                            .with(withRoles(KeyCloakRoles.MANAGER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 when role ADMIN")
        void save_adminRole_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test\"}")
                            .with(withRoles(KeyCloakRoles.ADMIN)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /{id} - ADMIN, MANAGER")
    class UpdateAuthTests {

        @Test
        @DisplayName("401 when no token provided")
        void update_noToken_returns401() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/{id}", TEST_ID)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 when role INTERN (not allowed)")
        void update_internRole_returns403() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/{id}", TEST_ID)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Test\"}")
                            .with(withRoles(KeyCloakRoles.INTERN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 when role ADMIN")
        void update_adminRole_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/{id}", TEST_ID)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Updated\"}")
                            .with(withRoles(KeyCloakRoles.ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 when role MANAGER")
        void update_managerRole_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put(BASE_URL + "/{id}", TEST_ID)
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"fullName\":\"Updated\"}")
                            .with(withRoles(KeyCloakRoles.MANAGER)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /search - ADMIN, MANAGER")
    class SearchAuthTests {

        @Test
        @DisplayName("401 when no token provided")
        void search_noToken_returns401() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/search"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 when role INTERN (not allowed)")
        void search_internRole_returns403() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/search")
                            .with(withRoles(KeyCloakRoles.INTERN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 when role ADMIN")
        void search_adminRole_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/search")
                            .with(withRoles(KeyCloakRoles.ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 when role MANAGER")
        void search_managerRole_returns200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/search")
                            .with(withRoles(KeyCloakRoles.MANAGER)))
                    .andExpect(status().isOk());
        }
    }
}
