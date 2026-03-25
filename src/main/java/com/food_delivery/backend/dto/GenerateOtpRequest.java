package com.food_delivery.backend.dto;

import com.food_delivery.backend.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateOtpRequest {

    private String email;
    private String phone;

    @NotNull
    private Role role;

    @AssertTrue(message = "Either email or phone is required")
    public boolean hasIdentifier() {
        return (email != null && !email.isBlank()) || (phone != null && !phone.isBlank());
    }

    @AssertTrue(message = "Provide only one identifier: email or phone")
    public boolean hasSingleIdentifier() {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = phone != null && !phone.isBlank();
        return hasEmail ^ hasPhone;
    }
}
