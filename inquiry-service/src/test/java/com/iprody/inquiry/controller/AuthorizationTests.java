package com.iprody.inquiry.controller;


import com.iprody.common.ResultList;
import com.iprody.inquiry.KeyCloakRoles;
import com.iprody.inquiry.configuration.SecurityConfiguration;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.service.EventProcessorService;
import com.iprody.inquiry.service.InquiryFacade;
import com.iprody.inquiry.service.InquiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InquiryController.class)
@Import(SecurityConfiguration.class)
@DisplayName("🔐 Authorization tests")
class AuthorizationTests {
    private static final String BASE_URL = "/api/v1/inquires";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private InquiryFacade inquiryFacade;

    @MockitoBean
    private EventProcessorService eventProcessorService;

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
    @DisplayName("POST / - only ADMIN")
    class SaveAuthTests {

        @Test
        @DisplayName("401 when no token provided")
        void save_noToken_returns401() throws Exception {
            String validInquiryJson = """
                    {
                        "groupRefId": "%s",
                        "customerRefId": "%s",
                        "managerRefId": "%s",
                        "source": "WEB_SITE",
                        "numberOfSeats": 5
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validInquiryJson))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 when role MANAGER (not allowed)")
        void save_managerRole_returns403() throws Exception {
            String validInquiryJson = """
                    {
                        "groupRefId": "%s",
                        "customerRefId": "%s",
                        "managerRefId": "%s",
                        "source": "WEB_SITE",
                        "numberOfSeats": 5
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validInquiryJson)
                            .with(withRoles(KeyCloakRoles.MANAGER)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 when role ADMIN")
        void save_adminRole_returns200() throws Exception {
            String validInquiryJson = """
                    {
                        "groupRefId": "%s",
                        "customerRefId": "%s",
                        "managerRefId": "%s",
                        "source": "WEB_SITE",
                        "numberOfSeats": 5
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validInquiryJson)
                            .with(withRoles(KeyCloakRoles.ADMIN)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 when role ADMIN")
        void cancel_adminRole_returns200() throws Exception {
            String validCancelJson = """
                    {
                        "id": "%s",
                        "status": "RECEIVED",
                        "reason": "Customer changed their mind"
                    }
                    """.formatted(UUID.randomUUID());
            mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCancelJson)
                            .with(withRoles(KeyCloakRoles.ADMIN)))
                    .andExpect(status().isAccepted());
        }
    }

    @Nested
    @DisplayName("GET /search")
    class SearchInquiriesAuthTests {
        @Test
        @DisplayName("200 when role ADMIN")
        void search_adminRole_returns200() throws Exception {
            ResultList<Inquiry> mockResult = new com.iprody.common.ResultList<>(List.of(new Inquiry()), 1L);
            Mockito.when(inquiryService.findAllByFilter(Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(mockResult);

            mockMvc.perform(createSearchRequest()
                            .with(withRoles(KeyCloakRoles.ADMIN))) // Подставляем ADMIN
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.totalCount").value(1));
        }

        @Test
        @DisplayName("200 when role MANAGER")
        void search_managerRole_returns200() throws Exception {
            ResultList<Inquiry> mockResult = new ResultList<>(List.of(new Inquiry()), 1L);
            Mockito.when(inquiryService.findAllByFilter(Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(mockResult);

            mockMvc.perform(createSearchRequest()
                            .with(withRoles(KeyCloakRoles.MANAGER)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("403 when role INTERN (not allowed)")
        void search_userRole_returns403Forbidden() throws Exception {
            mockMvc.perform(createSearchRequest()
                            .with(withRoles(KeyCloakRoles.INTERN)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 when no token provided")
        void search_noToken_returns401Unauthorized() throws Exception {
            mockMvc.perform(createSearchRequest())
                    .andExpect(status().isUnauthorized());
        }
    }

    private MockHttpServletRequestBuilder createSearchRequest() {
        return MockMvcRequestBuilders.get(BASE_URL + "/search")
                .param("filter.status", "NEW")
                .param("filter.customerRefId", UUID.randomUUID().toString())
                .param("filter.managerRefId", UUID.randomUUID().toString())
                .param("pagination.offset", "0")
                .param("pagination.limit", "10")
                .param("sorting.sortField", "STATUS")
                .param("sorting.sortDirection", "DESC");
    }
}
