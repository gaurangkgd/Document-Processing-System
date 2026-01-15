package com.docprocessor.system.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class LoginRequestDTO {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
