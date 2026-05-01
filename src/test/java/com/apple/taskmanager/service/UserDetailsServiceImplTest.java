package com.apple.taskmanager.service;

import com.apple.taskmanager.entity.Role;
import com.apple.taskmanager.entity.User;
import com.apple.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        User user = new User("alice", "encoded-pass", "Alice Smith", "alice@example.com", Role.USER);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("encoded-pass");
    }

    @Test
    void loadUserByUsername_existingUser_hasCorrectRoleAuthority() {
        User user = new User("mgr", "pass", "Manager One", null, Role.MANAGER);

        when(userRepository.findByUsername("mgr")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("mgr");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_MANAGER");
    }

    @Test
    void loadUserByUsername_adminUser_hasAdminAuthority() {
        User user = new User("admin", "pass", "Admin User", null, Role.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nobody");
    }

    @Test
    void loadUserByUsername_callsRepositoryWithCorrectUsername() {
        User user = new User("user1", "pass", "User One", null, Role.USER);
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

        userDetailsService.loadUserByUsername("user1");

        verify(userRepository).findByUsername("user1");
    }
}