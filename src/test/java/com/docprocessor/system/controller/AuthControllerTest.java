package com.docprocessor.system.controller;

import com.docprocessor.system.dto.LoginRequestDTO;
import com.docprocessor.system.dto.UserRegistrationDTO;
import com.docprocessor.system.model.Role;
import com.docprocessor.system.model.User;
import com.docprocessor.system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    @Test
    void testRegister_Success() throws Exception {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("testuser");
        dto.setEmail("testuser@example.com");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@example.com"));

        // verify saved to database
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        userRepository.findByUsername("testuser").ifPresent(user -> {
            assertThat(user.getEmail()).isEqualTo("testuser@example.com");
        });
    }

    @Test
    void testRegister_DuplicateUsername() throws Exception {
        // save existing user
        User existing = new User();
        existing.setUsername("dupuser");
        existing.setEmail("dupuser@example.com");
        existing.setPassword("secret");
        existing.setRole(Role.USER);
        userRepository.save(existing);

        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("dupuser");
        dto.setEmail("newemail@example.com");
        dto.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void testLogin_Success() throws Exception {
        // register via endpoint to ensure password encoding
        UserRegistrationDTO reg = new UserRegistrationDTO();
        reg.setUsername("loginuser");
        reg.setEmail("loginuser@example.com");
        reg.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername("loginuser");
        login.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void testLogin_WrongPassword() throws Exception {
        // register via endpoint
        UserRegistrationDTO reg = new UserRegistrationDTO();
        reg.setUsername("wrongpassuser");
        reg.setEmail("wrongpass@example.com");
        reg.setPassword("rightpassword");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequestDTO login = new LoginRequestDTO();
        login.setUsername("wrongpassuser");
        login.setPassword("badpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}
