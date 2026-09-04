package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.LoginRequest;
import com.beesagono.backend.dto.auth.LoginResponse;
import com.beesagono.backend.dto.auth.RegisterRequest;
import com.beesagono.backend.dto.auth.RegisterResponse;
import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.repository.RoleRepository;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.repository.UserRoleRepository;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private TokenBlacklist tokenBlacklist;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("register - Success")
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Role role = Role.builder().id("role-1").name(RoleName.ROLE_USER).build();
        User savedUser = User.builder()
                .id("user-1")
                .username("testuser")
                .email("test@example.com")
                .userRoles(new ArrayList<>())
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("user-1");
        assertThat(response.getUsername()).isEqualTo("testuser");
        verify(userRoleRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("register - Throws Exception when email exists")
    void register_EmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email già in uso");
    }

    @Test
    @DisplayName("login - Success")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");

        UserDetailsImpl userDetails = new UserDetailsImpl("user-1", "testuser", "test@example.com", "pwd",
                Collections.emptyList());
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtToken(userDetails)).thenReturn("jwt.token.value");

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt.token.value");
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("logout - Adds token to blacklist")
    void logout_Success() {
        String token = "Bearer sample.jwt.token";
        Instant expiry = Instant.now().plusSeconds(3600);

        when(jwtUtils.extractExpiry("sample.jwt.token")).thenReturn(expiry);

        authService.logout(token);

        verify(tokenBlacklist, times(1)).add("sample.jwt.token", expiry);
    }
}