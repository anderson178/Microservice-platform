package com.iprody.customer.repository;

import com.iprody.customer.model.Contract;
import com.iprody.customer.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql(scripts = {"/sql/init-schema.sql"})
class CustomerRepoTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("sql/init-schema.sql");

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private ContractRepo contractRepo;

    @Autowired
    private TestEntityManager entityManager;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        customerRepo.deleteAll();
        contractRepo.deleteAll();
        entityManager.flush();
        entityManager.clear();

        Contract testContract = new Contract("test@example.com", "+79991234567");
        testContract.setUpdatedAt(Timestamp.valueOf("2024-02-01 10:00:00"));
        testContract.setCreatedAt(Timestamp.valueOf("2024-02-01 10:00:00"));
        testCustomer = new Customer("Ivan Ivanov", testContract);
        testCustomer.setUpdatedAt(Timestamp.valueOf("2024-02-01 10:00:00"));
        testCustomer.setCreatedAt(Timestamp.valueOf("2024-02-01 10:00:00"));
    }

    @Test
    @DisplayName("save: persists customer with contract (cascade ALL)")
    void save_persistsCustomerWithContract() {
        var savedCustomer = customerRepo.save(testCustomer);
        entityManager.flush();
        entityManager.clear();

        assertThat(savedCustomer.getId()).isNotNull();
        assertThat(savedCustomer.getContract().getId()).isNotNull();
        assertThat(savedCustomer.getCreatedAt()).isNotNull();
        assertThat(savedCustomer.getUpdatedAt()).isNotNull();

        var found = customerRepo.findById(savedCustomer.getId());
        assertThat(found)
                .isPresent()
                .hasValueSatisfying(c -> {
                    assertThat(c.getFullName()).isEqualTo("Ivan Ivanov");
                    assertThat(c.getContract().getEmail()).isEqualTo("test@example.com");
                    assertThat(c.getContract().getPhoneNumber()).isEqualTo("+79991234567");
                });
    }

    @Test
    @DisplayName("findById: loads customer with contract (EAGER fetch)")
    void findById_loadsCustomerWithContract() {
        var saved = customerRepo.save(testCustomer);
        entityManager.flush();
        entityManager.clear();

        Optional<Customer> found = customerRepo.findById(saved.getId());

        assertThat(found).isPresent();
        Customer customer = found.get();
        assertThat(customer.getContract()).isNotNull();
        assertThat(customer.getContract().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("deleteById: cascades delete to contract")
    void deleteById_cascadesDeleteToContract() {
        Customer savedCustomer = customerRepo.save(testCustomer);
        UUID contractId = savedCustomer.getContract().getId();
        entityManager.flush();
        entityManager.clear();

        customerRepo.deleteById(savedCustomer.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(customerRepo.findById(savedCustomer.getId())).isEmpty();
        assertThat(contractRepo.findById(contractId)).isEmpty();
    }

    @Test
    @DisplayName("save: fails when email is null (NOT NULL constraint)")
    void save_failsWhenEmailIsNull() {
        Contract invalidContract = new Contract(null, "+79991234567");
        Customer invalidCustomer = new Customer("Test", invalidContract);

        assertThatThrownBy(() -> customerRepo.save(invalidCustomer))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("not-null property references a null or transient value for entity com.iprody.customer.model.Contract.email");
    }

    @Test
    @DisplayName("save: updates updatedAt on modification")
    void save_updatesTimestampsOnModification() throws InterruptedException {
        Customer saved = customerRepo.save(testCustomer);
        Timestamp originalUpdatedAt = saved.getUpdatedAt();
        entityManager.flush();
        entityManager.clear();

        Thread.sleep(10);
        saved.setFullName("Пётр Petrov");
        Customer updated = customerRepo.save(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getFullName()).isEqualTo("Пётр Petrov");
    }

    @Test
    @DisplayName("findByFullName: returns customers matching name")
    void findByFullName_returnsMatchingCustomers() {
        customerRepo.save(new Customer("Ivan Ivanov", new Contract("a@b.c", "111")));
        customerRepo.save(new Customer("Ivan Sidor", new Contract("d@e.f", "222")));
        customerRepo.save(new Customer("Пётр Petrov", new Contract("g@h.i", "333")));
        var page = customerRepo.findAllByFullNameContainingIgnoreCase("Ivan", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .extracting(Customer::getFullName)
                .containsExactlyInAnyOrder("Ivan Ivanov", "Ivan Sidor");
    }
}