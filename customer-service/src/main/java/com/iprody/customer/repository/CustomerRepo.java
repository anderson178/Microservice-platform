package com.iprody.customer.repository;

import com.iprody.customer.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, UUID> {
    Page<Customer> findAllByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
}
