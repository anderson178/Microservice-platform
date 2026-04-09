package com.iprody.customer.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.NonNull;

/**
 * The class is used to create and update data.
 */
@Data
public class ContractDataDto {
    @NonNull
    @Email(message = "Invalid email format")
    private String email;
    private String phoneNumber;
}
