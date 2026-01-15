package com.docprocessor.system.service;

import com.docprocessor.system.dto.AuthResponseDTO;
import com.docprocessor.system.dto.LoginRequestDTO;
import com.docprocessor.system.dto.UserRegistrationDTO;
import com.docprocessor.system.dto.UserResponseDTO;
import com.docprocessor.system.exception.AuthenticationException;
import com.docprocessor.system.exception.DuplicateResourceException;
import com.docprocessor.system.exception.ResourceNotFoundException;
import com.docprocessor.system.model.Role;
import com.docprocessor.system.model.User;
import com.docprocessor.system.repository.UserRepository;
import com.docprocessor.system.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService; // injected email service

    @Value("${jwt.expiration}")
    private long jwtExpiration = 0L; // default to avoid IDE warning when property isn't analyzed

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    public UserResponseDTO registerUser(UserRegistrationDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists: " + dto.getUsername());
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already exists: " + dto.getEmail());
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        // send welcome email (best-effort)
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());
        } catch (Exception ex) {
            // Log or swallow; do not prevent registration on email failures
            log.warn("Failed to send welcome email to {}: {}", savedUser.getEmail(), ex.getMessage());
        }

        return mapToResponseDTO(savedUser);
    }

    public AuthResponseDTO authenticateUser(LoginRequestDTO dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + dto.getUsername()));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        UserResponseDTO userDto = mapToResponseDTO(user);
        return new AuthResponseDTO(token, userDto, jwtExpiration);
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToResponseDTO(user);
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        // set Role enum directly (UserResponseDTO.setRole expects Role)
        dto.setRole(user.getRole());
        return dto;
    }
}
