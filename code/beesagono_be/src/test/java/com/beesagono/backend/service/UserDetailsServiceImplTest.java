package com.beesagono.backend.service;

import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.UserDetailsServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("loadUserByUsername - Success")
    void loadUserByUsername_Success() {
        String identifier = "testuser";
        User mockUser = new User();
        mockUser.setId("user-1");
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setPasswordHash("encodedPassword");
        mockUser.setUserRoles(Collections.emptyList());

        when(userRepository.findByUsernameOrEmail(identifier, identifier))
                .thenReturn(java.util.Optional.of(mockUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");

        verify(userRepository, times(1)).findByUsernameOrEmail(identifier, identifier);
    }

    @Test
    @DisplayName("loadUserByUsername - Throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_NotFound() {
        String identifier = "unknown";

        when(userRepository.findByUsernameOrEmail(identifier, identifier))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(identifier))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Utente non trovato con username o email: " + identifier);

        verify(userRepository, times(1)).findByUsernameOrEmail(identifier, identifier);
    }
}