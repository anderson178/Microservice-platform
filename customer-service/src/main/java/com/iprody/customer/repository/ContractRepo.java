package com.iprody.customer.repository;

import com.iprody.customer.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractRepo extends JpaRepository<Contract, UUID> {
}
