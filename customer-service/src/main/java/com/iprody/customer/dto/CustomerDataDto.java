package com.iprody.customer.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;

/**
 * The class is used to create and update data.
 */
@Data
public class CustomerDataDto {
    @NonNull
    @Size(max = 30, message = "Name is too long (max 30 symbols)")
    private String fullName;
    private ContractDataDto contract;
}
