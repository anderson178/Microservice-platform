package com.iprody.customer.service;

import com.iprody.common.*;
import com.iprody.common.exception.AppException;
import com.iprody.customer.model.*;
import com.iprody.customer.repository.ContractRepo;
import com.iprody.customer.repository.CustomerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepo customerRepo;
    private final ContractRepo contractRepo;

    @Transactional
    public Customer save(CustomerData customerData) {
        ContractData contractData = customerData.getContract();
        Contract contractSaved = contractRepo.save(new Contract(contractData.getEmail(), contractData.getPhoneNumber()));
        return customerRepo.save(new Customer(customerData.getFullName(), contractSaved));
    }

    @Transactional(readOnly = true)
    public Customer findById(UUID id) {
        return customerRepo.findById(id).orElseThrow(() ->  new AppException(ResultCode.NOT_FOUND));
    }

    public ResultList<Customer> findAllByFilter(CustomerFilter filter, Pagination pagination, Sorting sorting) {
        return ResultList.from(customerRepo.findAllByFullNameContainingIgnoreCase(
                filter.getFullName(),
                PageUtils.of(pagination, sorting))
        );
    }

    @Transactional
    public Customer update(UUID id, CustomerData customerData) {
        Customer customerDb = findById(id);
        customerDb.setFullName(customerData.getFullName());
        customerDb.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        customerDb.getContract().setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        customerDb.getContract().setEmail(customerData.getContract().getEmail());
        customerDb.getContract().setPhoneNumber(customerData.getContract().getPhoneNumber());
        return customerRepo.save(customerDb);
    }

    @Transactional
    public void delete(UUID id) {
        customerRepo.delete(findById(id));
    }
}
