package com.docprocessor.system.service;

import com.docprocessor.system.dto.AuthResponseDTO;
import com.docprocessor.system.dto.LoginRequestDTO;
import com.docprocessor.system.dto.UserRegistrationDTO;
import com.docprocessor.system.dto.UserResponseDTO;
import com.docprocessor.system.exception.AuthenticationException;
import com.docprocessor.system.exception.DuplicateResourceException;
import com.docprocessor.system.model.Role;
import com.docprocessor.system.model.User;
import com.docprocessor.system.repository.UserRepository;
import com.docprocessor.system.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    public void testRegisterUser_Success() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("john");
        dto.setEmail("john@example.com");
        dto.setPassword("secret123");

        when(userRepository.findByUsername(dto.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encrypted");

        User saved = new User();
        saved.setId(1L);
        saved.setUsername(dto.getUsername());
        saved.setEmail(dto.getEmail());
        saved.setPassword("encrypted");
        saved.setRole(Role.USER);

        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponseDTO result = userService.registerUser(dto);

        verify(userRepository, times(1)).save(any(User.class));
        assertNotNull(result);
        assertEquals(saved.getUsername(), result.getUsername());
        assertEquals(saved.getEmail(), result.getEmail());
    }

    @Test
    public void testRegisterUser_DuplicateUsername() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("john");
        dto.setEmail("john@example.com");
        dto.setPassword("secret123");

        User existing = new User();
        existing.setId(2L);
        existing.setUsername(dto.getUsername());

        when(userRepository.findByUsername(dto.getUsername())).thenReturn(Optional.of(existing));

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(dto));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testAuthenticateUser_Success() {
        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername("john");
        login.setPassword("secret123");

        User user = new User();
        user.setId(1L);
        user.setUsername(login.getUsername());
        user.setPassword("encrypted");
        user.setRole(Role.USER);

        when(userRepository.findByUsername(login.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(login.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user.getUsername(), user.getRole().name())).thenReturn("token123");

        AuthResponseDTO resp = userService.authenticateUser(login);

        assertNotNull(resp);
        assertEquals("token123", resp.getToken());
        assertNotNull(resp.getUser());
        assertEquals(user.getUsername(), resp.getUser().getUsername());
    }

    @Test
    public void testAuthenticateUser_WrongPassword() {
        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername("john");
        login.setPassword("wrongpass");

        User user = new User();
        user.setId(1L);
        user.setUsername(login.getUsername());
        user.setPassword("encrypted");
        user.setRole(Role.USER);

        when(userRepository.findByUsername(login.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(login.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> userService.authenticateUser(login));
    }
}

