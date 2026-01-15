package com.docprocessor.system.dto;

import com.docprocessor.system.model.Role;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private Role role;
}
