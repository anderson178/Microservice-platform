package com.iprody.customer.repository;

import com.iprody.customer.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContractRepo extends JpaRepository<Contract, UUID> {
}
