package com.docprocessor.system.repository;

import com.docprocessor.system.model.Role;
import com.docprocessor.system.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testFindByUsername_Success() {
        User user = new User();
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByUsername("johndoe");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void testFindByUsername_NotFound() {
        Optional<User> found = userRepository.findByUsername("nonexistent");
        assertThat(found).isNotPresent();
    }

    @Test
    void testExistsByUsername() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isFalse();
    }

    @Test
    void testFindByEmail() {
        User user = new User();
        user.setUsername("charlie");
        user.setEmail("charlie@example.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmail("charlie@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("charlie");
    }
}
