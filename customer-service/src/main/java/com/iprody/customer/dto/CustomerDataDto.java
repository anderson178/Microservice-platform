package com.iprody.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * The class is used to create and update data.
 */
@Data
public class CustomerDataDto {
    @NotBlank
    @Size(max = 30, message = "Name is too long (max 30 symbols)")
    private String fullName;
    private ContractDataDto contract;

    public CustomerDataDto(String fullName) {
        this.fullName = fullName;
    }
}
