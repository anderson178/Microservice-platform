package com.iprody.customer.controller;

import com.iprody.common.ResultList;
import com.iprody.customer.dto.CustomerDataDto;
import com.iprody.customer.model.Customer;
import com.iprody.customer.service.CustomerService;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

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

    @Test
    void save_Success() throws Exception {
        CustomerDataDto inputDto = new CustomerDataDto("New Customer");

        Customer savedCustomer = new Customer();
        savedCustomer.setId(UUID.randomUUID());
        savedCustomer.setFullName("New Customer");

        when(customerService.save(any())).thenReturn(savedCustomer);
        // When & Then
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(new ObjectMapper().writeValueAsString(inputDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Customer"))
                .andExpect(jsonPath("$.id").exists());

        verify(customerService).save(any());
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
                        .content(new ObjectMapper().writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));

        verify(customerService).update(eq(id), any());
    }
}