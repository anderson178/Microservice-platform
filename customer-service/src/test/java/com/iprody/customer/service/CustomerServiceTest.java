package com.iprody.customer.service;

import com.iprody.common.Pagination;
import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.common.exception.AppException;
import com.iprody.customer.model.*;
import com.iprody.customer.repository.ContractRepo;
import com.iprody.customer.repository.CustomerRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService tests")
class CustomerServiceTest {
    @Mock
    private CustomerRepo customerRepo;

    @Mock
    private ContractRepo contractRepo;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void save_ShouldReturnSavedCustomer() {
        CustomerData data = createCustomerData();
        Contract contract = new Contract(data.getContract().getEmail(), data.getFullName());
        Customer customer = new Customer(data.getFullName(), contract);

        when(contractRepo.save(any(Contract.class))).thenReturn(contract);
        when(customerRepo.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.save(data);

        assertNotNull(result);
        assertEquals("Ivanov", result.getFullName());
        verify(contractRepo).save(any(Contract.class));
        verify(customerRepo).save(any(Customer.class));
    }

    @Test
    void findById_ShouldReturnCustomer_WhenExists() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);

        when(customerRepo.findById(id)).thenReturn(Optional.of(customer));

        Customer result = customerService.findById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void findById_ShouldThrowException_WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepo.findById(id)).thenReturn(Optional.empty());
        AppException exception = assertThrows(AppException.class, () -> customerService.findById(id));
        assertEquals(ResultCode.NOT_FOUND, exception.getCode());
    }

    @Test
    void delete_ShouldCallDelete_WhenCustomerExists() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        when(customerRepo.findById(id)).thenReturn(Optional.of(customer));
        customerService.delete(id);
        verify(customerRepo, times(1)).delete(customer);
    }

    @Test
    void findAllByFilter_ShouldReturnResultList() {
        String fullName = "Ivanov";
        CustomerFilter filter = new CustomerFilter();
        filter.setFullName(fullName);
        Pagination pagination = new Pagination(0, 10);
        Customer customer = new Customer();
        customer.setFullName(fullName);
        Page<Customer> customerPage = new PageImpl<>(List.of(customer));

        when(customerRepo.findAllByFullNameContainingIgnoreCase(eq(fullName), any(Pageable.class)))
                .thenReturn(customerPage);

        ResultList<Customer> result = customerService.findAllByFilter(filter, pagination);

        assertNotNull(result);
        assertEquals(1, result.getElements().size());
        assertEquals(fullName, result.getElements().get(0).getFullName());
        verify(customerRepo).findAllByFullNameContainingIgnoreCase(eq(fullName), any(Pageable.class));
    }

    private CustomerData createCustomerData() {
        CustomerData data = new CustomerData();
        data.setFullName("Ivanov");
        ContractData contractData = new ContractData();
        contractData.setEmail("test@mail.com");
        data.setContract(contractData);
        return data;
    }

}