package com.iprody.customer.service;

import com.iprody.common.Pagination;
import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.common.Sorting;
import com.iprody.common.exception.AppException;
import com.iprody.customer.model.*;
import com.iprody.customer.repository.ContractRepo;
import com.iprody.customer.repository.CustomerRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

    @Nested
    @DisplayName("save()")
    class SaveTests {
        @Test
        @DisplayName("should save customer with contract when data is valid")
        void save_validData_returnsSavedCustomer() {
            CustomerData data = createCustomerData();
            Contract savedContract = new Contract(data.getContract().getEmail(), data.getContract().getPhoneNumber());
            savedContract.setId(UUID.randomUUID());

            Customer savedCustomer = new Customer(data.getFullName(), savedContract);
            savedCustomer.setId(UUID.randomUUID());
            savedCustomer.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

            when(contractRepo.save(any(Contract.class))).thenReturn(savedContract);
            when(customerRepo.save(any(Customer.class))).thenReturn(savedCustomer);

            Customer result = customerService.save(data);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getFullName()).isEqualTo("Ivan");
            assertThat(result.getContract()).isNotNull();
            assertThat(result.getContract().getEmail()).isEqualTo("test@mail.com");

            verify(contractRepo).save(argThat(c ->
                    c.getEmail().equals("test@mail.com") && c.getPhoneNumber().equals("+79991234567")));
            verify(customerRepo).save(argThat(c ->
                    c.getFullName().equals("Ivan") && c.getContract() != null));
        }

        @Test
        @DisplayName("should throw when customerData is null")
        void save_nullData_throwsException() {
            assertThatThrownBy(() -> customerService.save(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("customerData");

            verifyNoInteractions(contractRepo, customerRepo);
        }

        @Test
        @DisplayName("should throw when contract is null")
        void save_nullContract_throwsException() {
            CustomerData data = createCustomerData();
            data.setContract(null);

            assertThatThrownBy(() -> customerService.save(data))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("contract");

            verifyNoInteractions(contractRepo, customerRepo);
        }

        @Test
        @DisplayName("should propagate database exception")
        void save_databaseError_propagatesException() {
            CustomerData data = createCustomerData();
            when(contractRepo.save(any(Contract.class)))
                    .thenThrow(new RuntimeException("DB connection failed"));

            assertThatThrownBy(() -> customerService.save(data))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB connection failed");

            verify(contractRepo).save(any(Contract.class));
            verifyNoInteractions(customerRepo);
        }

        @Test
        void save_ShouldReturnSavedCustomer() {
            CustomerData data = createCustomerData();
            Contract contract = new Contract(data.getContract().getEmail(), data.getFullName());
            Customer customer = new Customer(data.getFullName(), contract);

            when(contractRepo.save(any(Contract.class))).thenReturn(contract);
            when(customerRepo.save(any(Customer.class))).thenReturn(customer);

            Customer result = customerService.save(data);

            assertNotNull(result);
            assertEquals("Ivan", result.getFullName());
            verify(contractRepo).save(any(Contract.class));
            verify(customerRepo).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {
        @Test
        @DisplayName("should return customer when exists")
        void findById_exists_returnsCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = new Customer("Test User", new Contract("a@b.c", "123"));
            customer.setId(id);

            when(customerRepo.findById(id)).thenReturn(Optional.of(customer));

            Customer result = customerService.findById(id);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getFullName()).isEqualTo("Test User");
            verify(customerRepo).findById(id);
        }

        @Test
        @DisplayName("should throw AppException(NOT_FOUND) when customer not found")
        void findById_notFound_throwsAppException() {
            UUID id = UUID.randomUUID();
            when(customerRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findById(id))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getCode())
                    .isEqualTo(ResultCode.NOT_FOUND);

            verify(customerRepo).findById(id);
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
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {
        @Test
        @DisplayName("should update customer when data is valid")
        void update_validData_returnsUpdatedCustomer() {
            UUID id = UUID.randomUUID();
            Customer existing = new Customer("Old Name", new Contract("old@a.b", "111"));
            existing.setId(id);

            CustomerData updateData = new CustomerData();
            updateData.setFullName("New Name");
            ContractData contractData = new ContractData();
            contractData.setEmail("new@c.d");
            contractData.setPhoneNumber("222");
            updateData.setContract(contractData);

            when(customerRepo.findById(id)).thenReturn(Optional.of(existing));
            when(customerRepo.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Customer result = customerService.update(id, updateData);

            assertThat(result.getFullName()).isEqualTo("New Name");
            assertThat(result.getContract().getEmail()).isEqualTo("new@c.d");
            assertThat(result.getContract().getPhoneNumber()).isEqualTo("222");
            assertThat(result.getUpdatedAt()).isNotNull();

            verify(customerRepo).findById(id);
            verify(customerRepo).save(existing);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when customer doesn't exist")
        void update_notFound_throwsAppException() {
            UUID id = UUID.randomUUID();
            when(customerRepo.findById(id)).thenReturn(Optional.empty());
            CustomerData data = createCustomerData();

            assertThatThrownBy(() -> customerService.update(id, data))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getCode())
                    .isEqualTo(ResultCode.NOT_FOUND);

            verify(customerRepo).findById(id);
            verifyNoInteractions(contractRepo);
        }
    }

    @Nested
    @DisplayName("findAllByFilter()")
    class FindAllByFilterTests {

        @Test
        @DisplayName("should return ResultList with customers")
        void findAllByFilter_ShouldReturnResultList() {
            String fullName = "Ivanov";
            CustomerFilter filter = new CustomerFilter();
            filter.setFullName(fullName);
            Pagination pagination = new Pagination(0, 10);
            Sorting sorting = new Sorting(CustomerSortField.FULL_NAME, Sort.Direction.DESC);
            Customer customer = new Customer();
            customer.setFullName(fullName);
            Page<Customer> customerPage = new PageImpl<>(List.of(customer));

            when(customerRepo.findAllByFullNameContainingIgnoreCase(eq(fullName), any(Pageable.class)))
                    .thenReturn(customerPage);

            ResultList<Customer> result = customerService.findAllByFilter(filter, pagination, sorting);

            assertNotNull(result);
            assertEquals(1, result.getData().size());
            assertEquals(fullName, result.getData().get(0).getFullName());
            verify(customerRepo).findAllByFullNameContainingIgnoreCase(eq(fullName), any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty ResultList when no matches")
        void findAllByFilter_noMatches_returnsEmptyResultList() {
            CustomerFilter filter = new CustomerFilter();
            filter.setFullName("NonExistent");
            Pagination pagination = new Pagination(0, 10);
            Sorting sorting = new Sorting(CustomerSortField.FULL_NAME, Sort.Direction.ASC);

            Page<Customer> emptyPage = new PageImpl<>(List.of());
            when(customerRepo.findAllByFullNameContainingIgnoreCase(eq("NonExistent"), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            ResultList<Customer> result = customerService.findAllByFilter(filter, pagination, sorting);

            assertThat(result.getTotalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should delete customer when exists")
        void delete_ShouldCallDelete_WhenCustomerExists() {
            UUID id = UUID.randomUUID();
            Customer customer = new Customer();
            when(customerRepo.findById(id)).thenReturn(Optional.of(customer));
            customerService.delete(id);
            verify(customerRepo, times(1)).delete(customer);
        }

        @Test
        @DisplayName("should delete customer when exists")
        void delete_exists_deletesCustomer() {
            UUID id = UUID.randomUUID();
            Customer customer = new Customer("Test", new Contract("a@b.c", "123"));
            customer.setId(id);

            when(customerRepo.findById(id)).thenReturn(Optional.of(customer));

            customerService.delete(id);

            verify(customerRepo).findById(id);
            verify(customerRepo).delete(customer);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when customer doesn't exist")
        void delete_notFound_throwsAppException() {
            UUID id = UUID.randomUUID();
            when(customerRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.delete(id))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getCode())
                    .isEqualTo(ResultCode.NOT_FOUND);

            verify(customerRepo).findById(id);
            verify(customerRepo, never()).delete(any());
        }
    }

    private CustomerData createCustomerData() {
        CustomerData data = new CustomerData();
        data.setFullName("Ivan");
        ContractData contractData = new ContractData();
        contractData.setEmail("test@mail.com");
        contractData.setPhoneNumber("+79991234567");
        data.setContract(contractData);
        return data;
    }
}
