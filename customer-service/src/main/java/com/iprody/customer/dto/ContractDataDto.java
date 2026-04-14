package com.iprody.customer.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * The class is used to create and update data.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractDataDto {
    @NonNull
    @Email(message = "Invalid email format")
    private String email;
    private String phoneNumber;
}
