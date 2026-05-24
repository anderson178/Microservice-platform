package com.iprody.inquiry.controller;

import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.inquiry.KeyCloakRoles;
import com.iprody.inquiry.configuration.ConfigurationTest;
import com.iprody.inquiry.dto.InquiryDataDto;
import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquiryStatus;
import com.iprody.inquiry.service.EventProcessorService;
import com.iprody.inquiry.service.InquiryFacade;
import com.iprody.inquiry.service.InquiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InquiryController.class)
@Import(ConfigurationTest.class)
@DisplayName("InquiryController tests")
class InquiryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InquiryService inquiryService;

    @MockitoBean
    private EventProcessorService eventProcessorService;

    @MockitoBean
    private InquiryFacade inquiryFacade;


    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor withRoles(String... roles) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt
                        .claim("realm_access", Map.of("roles", List.of(roles)))
                        .claim("preferred_username", "test-user")
                )
                .authorities(Stream.of(roles)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toArray(org.springframework.security.core.GrantedAuthority[]::new)
                );
    }

    @Nested
    @DisplayName("POST /inquires")
    class SaveTests {

        @Test
        @DisplayName("200 when inquiry is saved successfully")
        void save_Success() throws Exception {
            InquiryDataDto inputDto = new InquiryDataDto();
            inputDto.setGroupRefId(UUID.randomUUID());
            inputDto.setCustomerRefId(UUID.randomUUID());
            inputDto.setManagerRefId(UUID.randomUUID());
            inputDto.setSource("WEB");
            inputDto.setNumberOfSeats(2L);

            Inquiry savedInquiry = new Inquiry();
            savedInquiry.setId(UUID.randomUUID());
            savedInquiry.setStatus(InquiryStatus.NEW);

            when(inquiryFacade.save(any(), anyString())).thenReturn(savedInquiry);

            mockMvc.perform(post("/api/v1/inquires")
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content(objectMapper.writeValueAsString(inputDto))
                            .with(withRoles(KeyCloakRoles.ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedInquiry.getId().toString()))
                    .andExpect(jsonPath("$.status").value(InquiryStatus.NEW.name()));

            verify(inquiryFacade).save(any(), anyString());
        }
    }

    @Nested
    @DisplayName("POST /inquires - Negative cases")
    class SaveNegativeTests {

        @Test
        @DisplayName("400 when managerRefId is blank")
        void save_NullCustomerRefId_returns400() throws Exception {
            InquiryDataDto invalidDto = new InquiryDataDto();

            mockMvc.perform(post("/api/v1/inquires")
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ResultCode.VALIDATION_ERROR.name()));

            verifyNoInteractions(inquiryService);
        }

        @Test
        @DisplayName("400 when request body is empty")
        void save_EmptyBody_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/inquires")
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(inquiryService);
        }

        @Test
        @DisplayName("424 when external service is unavailable")
        void save_externalServiceUnavailable_returns424() throws Exception {
            InquiryDataDto validDto = new InquiryDataDto();
            validDto.setGroupRefId(UUID.randomUUID());
            validDto.setCustomerRefId(UUID.randomUUID());
            validDto.setManagerRefId(UUID.randomUUID());
            validDto.setSource("WEB");
            validDto.setNumberOfSeats(2L);

            when(inquiryFacade.save(any(), anyString()))
                    .thenThrow(new ResourceAccessException("Connection refused: http://external-service/api"));

            mockMvc.perform(post("/api/v1/inquires")
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content(objectMapper.writeValueAsString(validDto)))
                    .andExpect(status().isFailedDependency())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON.toString()))
                    .andExpect(jsonPath("$.code").value(ResultCode.EXTERNAL_SERVICE_UNAVAILABLE.name()))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verify(inquiryFacade).save(any(), anyString());
        }
    }

    @Nested
    @DisplayName("GET /inquires/search")
    class SearchTests {

        @Test
        @DisplayName("200 when search returns results")
        void findAllByFilter_WithQueryParameters() throws Exception {
            ResultList<Inquiry> mockResult = new ResultList<>(List.of(new Inquiry()), 1L);
            when(inquiryService.findAllByFilter(any(), any(), any())).thenReturn(mockResult);

            mockMvc.perform(get("/api/v1/inquires/search")
                            .param("filter.status", "NEW")
                            .param("filter.customerRefId", UUID.randomUUID().toString())
                            .param("filter.managerRefId", UUID.randomUUID().toString())
                            .param("pagination.offset", "0")
                            .param("pagination.limit", "10")
                            .param("sorting.sortField", "STATUS")
                            .param("sorting.sortDirection", "DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.totalCount").value(1));

            verify(inquiryService).findAllByFilter(any(), any(), any());
        }

        @Test
        @DisplayName("200 when optional params are missing (defaults applied)")
        void search_MissingOptionalParams_usesDefaults() throws Exception {
            ResultList<Inquiry> mockResult = new ResultList<>(List.of(), 0L);
            when(inquiryService.findAllByFilter(any(), any(), any())).thenReturn(mockResult);

            mockMvc.perform(get("/api/v1/inquires/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());

            verify(inquiryService).findAllByFilter(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("GET /inquires/search - Negative cases")
    class SearchNegativeTests {

        @Test
        @DisplayName("400 when pagination.limit is negative")
        void search_NegativeLimit_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/inquires/search")
                            .param("pagination.limit", "-1")
                            .param("pagination.offset", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists());

            verifyNoInteractions(inquiryService);
        }

        @Test
        @DisplayName("400 when pagination.offset is negative")
        void search_NegativeOffset_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/inquires/search")
                            .param("pagination.limit", "10")
                            .param("pagination.offset", "-5"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(inquiryService);
        }

        @Test
        @DisplayName("400 when filter.customerRefId is invalid UUID")
        void search_InvalidCustomerRefId_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/inquires/search")
                            .param("filter.customerRefId", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsStringIgnoringCase("UUID")));

            verifyNoInteractions(inquiryService);
        }

        @Test
        @DisplayName("500 when service throws unexpected exception")
        void search_ServiceError_returns500() throws Exception {
            when(inquiryService.findAllByFilter(any(), any(), any()))
                    .thenThrow(new RuntimeException("Search failed"));

            mockMvc.perform(get("/api/v1/inquires/search")
                            .param("filter.status", InquiryStatus.NEW.name()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON.toString()))
                    .andExpect(jsonPath("$.code").value(ResultCode.INTERNAL_SERVER_ERROR.name()));

            verify(inquiryService).findAllByFilter(any(), any(), any());
        }
    }
}
