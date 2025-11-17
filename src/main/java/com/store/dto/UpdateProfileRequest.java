package com.store.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;
@Data
public class UpdateProfileRequest {
    private String name;
    @Email(message = "Email should be valid")
    private String email;
    private String phone;
}