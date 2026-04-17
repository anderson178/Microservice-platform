package com.iprody.customer.controller;

import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.common.exception.AppException;
import com.iprody.customer.configuration.ConfigurationTest;
import com.iprody.customer.dto.CustomerDataDto;
import com.iprody.customer.model.Customer;
import com.iprody.customer.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@Import(ConfigurationTest.class)
@DisplayName("CustomerController tests")
class CustomerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /")
    class SaveTests {
        @Test
        @DisplayName("400 when fullName is blank")
        void save_NullFullName_returns400() throws Exception {
            CustomerDataDto invalidDto = new CustomerDataDto("");

            mockMvc.perform(post("/api/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON.toString()))
                    .andExpect(jsonPath("$.code").value(ResultCode.VALIDATION_ERROR.name()))
                    .andExpect(jsonPath("$.message").value(containsString("fullName=must not be blank")));

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("400 when request body is empty")
        void save_EmptyBody_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        void save_Success() throws Exception {
            CustomerDataDto inputDto = new CustomerDataDto("New Customer");

            Customer savedCustomer = new Customer();
            savedCustomer.setId(UUID.randomUUID());
            savedCustomer.setFullName("New Customer");

            when(customerService.save(any())).thenReturn(savedCustomer);
            mockMvc.perform(post("/api/v1/customers")
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content(objectMapper.writeValueAsString(inputDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("New Customer"))
                    .andExpect(jsonPath("$.id").exists());

            verify(customerService).save(any());
        }
    }

    @Nested
    @DisplayName("PUT /{id}")
    class UpdateTests {
        @Test
        @DisplayName("404 when customer not found for update")
        void update_NotFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerDataDto dto = new CustomerDataDto("Updated Name");

            when(customerService.update(eq(id), any()))
                    .thenThrow(new AppException(ResultCode.NOT_FOUND, id));

            mockMvc.perform(put("/api/v1/customers/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ResultCode.NOT_FOUND.name()));

            verify(customerService).update(eq(id), any());
        }

        @Test
        void update_Success() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerDataDto updateDto = new CustomerDataDto("Updated Name");

            Customer updatedCustomer = new Customer();
            updatedCustomer.setId(id);
            updatedCustomer.setFullName("Updated Name");

            when(customerService.update(eq(id), any())).thenReturn(updatedCustomer);

            mockMvc.perform(put("/api/v1/customers/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON.toString())
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Updated Name"));

            verify(customerService).update(eq(id), any());
        }
    }

    @Nested
    @DisplayName("GET /search")
    class SearchTests {
        @Test
        @DisplayName("500 when service throws unexpected exception")
        void getById_ServiceError_returns500() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerService.findById(id))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(get("/api/v1/customers/{id}", id))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON.toString()))
                    .andExpect(jsonPath("$.code").value(ResultCode.INTERNAL_SERVER_ERROR.name()));

            verify(customerService).findById(id);
        }

        @Test
        @DisplayName("404 when customer not found")
        void getById_NotFound_returns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(customerService.findById(nonExistentId))
                    .thenThrow(new AppException(ResultCode.NOT_FOUND, nonExistentId));

            mockMvc.perform(get("/api/v1/customers/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON.toString()))
                    .andExpect(jsonPath("$.code").value(ResultCode.NOT_FOUND.name()))
                    .andExpect(jsonPath("$.message").exists());

            verify(customerService).findById(nonExistentId);
        }

        @Test
        @DisplayName("400 when pagination.offset is negative")
        void search_NegativeOffset_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/customers/search")
                            .param("pagination.limit", "10")
                            .param("pagination.offset", "-5"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("400 when UUID format is invalid")
        void getById_InvalidUuidFormat_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/customers/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON.toString()))
                    .andExpect(jsonPath("$.message").value(
                            containsStringIgnoringCase("UUID")
                    ));

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("400 when pagination.limit is negative")
        void search_NegativeLimit_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/customers/search")
                            .param("pagination.limit", "-1")
                            .param("pagination.offset", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").exists());

            verifyNoInteractions(customerService);
        }

        @Test
        void getById_Success() throws Exception {
            UUID id = UUID.randomUUID();
            Customer customer = new Customer();
            customer.setId(id);
            customer.setFullName("Ivanov Ivan");

            when(customerService.findById(id)).thenReturn(customer);

            mockMvc.perform(get("/api/v1/customers/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON.toString()))
                    .andExpect(jsonPath("$.fullName").value("Ivanov Ivan"));

            verify(customerService).findById(id);
        }

        @Test
        void findAllByFilter_WithQueryParameters() throws Exception {
            ResultList<Customer> mockResult = new ResultList<>(List.of(new Customer()), 1L);

            when(customerService.findAllByFilter(any(), any(), any()))
                    .thenReturn(mockResult);

            mockMvc.perform(get("/api/v1/customers/search")
                            .param("filter.fullName", "Ivan")
                            .param("pagination.offset", "0")
                            .param("pagination.limit", "10")
                            .param("sorting.sortField", "FULL_NAME")
                            .param("sorting.sortDirection", "ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());

            verify(customerService).findAllByFilter(any(), any(), any());
        }
    }
}
