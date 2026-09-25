package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.GoogleCheckResponse;
import com.beesagono.backend.dto.auth.GoogleLoginRequest;
import com.beesagono.backend.dto.auth.GoogleRegisterRequest;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock
    private JwtDecoder googleJwtDecoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Nested
    @DisplayName("register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully when request is valid")
        void shouldRegisterUserSuccessfullyWhenRequestIsValid() {
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
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getRole()).isEqualTo("ROLE_USER");
            verify(userRoleRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email già in uso");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameAlreadyExists() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setUsername("testuser");

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Username già in uso");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when default user role is not found")
        void shouldThrowExceptionWhenDefaultRoleNotFound() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail("test@example.com");
            request.setPassword("password123");

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Ruolo ROLE_USER non trovato"); // Stringa corretta

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should return login response with token when credentials are valid")
        void shouldReturnLoginResponseWhenCredentialsAreValid() {
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

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("jwt.token.value");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception when authentication fails due to invalid credentials")
        void shouldThrowExceptionWhenCredentialsAreInvalid() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Credenziali non valide"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenziali non valide");
        }
    }

    @Nested
    @DisplayName("logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should add token to blacklist when logout request contains valid Bearer token")
        void shouldAddTokenToBlacklistWhenTokenIsValid() {
            String token = "Bearer sample.jwt.token";
            String pureToken = "sample.jwt.token";
            Instant expiry = Instant.now().plusSeconds(3600);

            when(jwtUtils.validateJwtToken(pureToken)).thenReturn(true);
            when(jwtUtils.extractExpiry(pureToken)).thenReturn(expiry);

            authService.logout(token);

            verify(tokenBlacklist, times(1)).add(pureToken, expiry);
        }

        @Test
        @DisplayName("Should not add token to blacklist when token is invalid")
        void shouldNotBlacklistWhenTokenIsInvalid() {
            String token = "Bearer invalid.jwt.token";
            String pureToken = "invalid.jwt.token";

            when(jwtUtils.validateJwtToken(pureToken)).thenReturn(false);

            authService.logout(token);

            verify(tokenBlacklist, never()).add(anyString(), any());
        }

        @Test
        @DisplayName("Should handle logout gracefully when authorization header is missing Bearer prefix")
        void shouldHandleLogoutGracefullyWhenHeaderLacksBearerPrefix() {
            String token = "sample.jwt.token";

            authService.logout(token);

            verify(jwtUtils, never()).validateJwtToken(anyString());
            verify(tokenBlacklist, never()).add(anyString(), any());
        }
    }

    @Nested
    @DisplayName("checkGoogleUser Tests")
    class CheckGoogleUserTests {

        @Test
        @DisplayName("Should return registered status with login response when Google user already exists")
        void shouldReturnLoginResponseWhenGoogleUserExists() {
            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("mock.google.id.token");

            Jwt jwt = mock(Jwt.class);
            when(jwt.getClaimAsString("email")).thenReturn("googleuser@example.com");
            when(googleJwtDecoder.decode("mock.google.id.token")).thenReturn(jwt);

            User existingUser = User.builder()
                    .id("google-user-1")
                    .username("googleuser")
                    .email("googleuser@example.com")
                    .userRoles(new ArrayList<>())
                    .build();

            when(userRepository.findByEmail("googleuser@example.com")).thenReturn(Optional.of(existingUser));
            when(jwtUtils.generateJwtToken(any(UserDetailsImpl.class))).thenReturn("jwt.google.token");

            GoogleCheckResponse response = authService.checkGoogleUser(request);

            assertThat(response.isRegistered()).isTrue();
            assertThat(response.getLoginResponse()).isNotNull();
            assertThat(response.getLoginResponse().getAccessToken()).isEqualTo("jwt.google.token");
            assertThat(response.getLoginResponse().getEmail()).isEqualTo("googleuser@example.com");
        }

        @Test
        @DisplayName("Should return unregistered status with suggested username when Google user does not exist")
        void shouldReturnSuggestedUsernameWhenGoogleUserDoesNotExist() {
            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("mock.google.id.token");

            Jwt jwt = mock(Jwt.class);
            when(jwt.getClaimAsString("email")).thenReturn("mario.rossi@example.com");
            when(jwt.getClaimAsString("given_name")).thenReturn("Mario");
            when(jwt.getClaimAsString("family_name")).thenReturn("Rossi");
            when(googleJwtDecoder.decode("mock.google.id.token")).thenReturn(jwt);

            when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.empty());

            GoogleCheckResponse response = authService.checkGoogleUser(request);

            assertThat(response.isRegistered()).isFalse();
            assertThat(response.getEmail()).isEqualTo("mario.rossi@example.com");
            assertThat(response.getSuggestedUsername()).isEqualTo("mariorossi");
            assertThat(response.getFirstName()).isEqualTo("Mario");
            assertThat(response.getLastName()).isEqualTo("Rossi");
        }

        @Test
        @DisplayName("Should throw exception when Google ID token decoding fails")
        void shouldThrowExceptionWhenGoogleTokenIsInvalid() {
            GoogleLoginRequest request = new GoogleLoginRequest();
            request.setIdToken("invalid.google.token");

            when(googleJwtDecoder.decode("invalid.google.token")).thenThrow(new JwtException("Invalid token"));

            assertThatThrownBy(() -> authService.checkGoogleUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Token Google non valido");
        }
    }

    @Nested
    @DisplayName("registerGoogleUser Tests")
    class RegisterGoogleUserTests {

        @Test
        @DisplayName("Should register new Google user successfully when request is valid")
        void shouldRegisterGoogleUserSuccessfullyWhenRequestIsValid() {
            GoogleRegisterRequest request = new GoogleRegisterRequest();
            request.setIdToken("mock.google.id.token");
            request.setUsername("chosen_username");

            Jwt jwt = mock(Jwt.class);
            when(jwt.getClaimAsString("email")).thenReturn("newgoogleuser@example.com");
            when(googleJwtDecoder.decode("mock.google.id.token")).thenReturn(jwt);

            when(userRepository.existsByEmail("newgoogleuser@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("chosen_username")).thenReturn(false);

            Role role = Role.builder().id("role-1").name(RoleName.ROLE_USER).build();
            when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(anyString())).thenReturn("randomPasswordHash");

            User savedUser = User.builder()
                    .id("google-user-2")
                    .username("chosen_username")
                    .email("newgoogleuser@example.com")
                    .userRoles(new ArrayList<>())
                    .build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtUtils.generateJwtToken(any(UserDetailsImpl.class))).thenReturn("jwt.google.token");

            LoginResponse response = authService.registerGoogleUser(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("jwt.google.token");
            assertThat(response.getUsername()).isEqualTo("chosen_username");
            assertThat(response.getEmail()).isEqualTo("newgoogleuser@example.com");
            verify(userRoleRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Should throw exception when registering Google user with an already existing email")
        void shouldThrowExceptionWhenGoogleEmailAlreadyExists() {
            GoogleRegisterRequest request = new GoogleRegisterRequest();
            request.setIdToken("mock.google.id.token");
            request.setUsername("chosen_username");

            Jwt jwt = mock(Jwt.class);
            when(jwt.getClaimAsString("email")).thenReturn("existing@example.com");
            when(googleJwtDecoder.decode("mock.google.id.token")).thenReturn(jwt);

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerGoogleUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email già registrata");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when registering Google user with an already taken username")
        void shouldThrowExceptionWhenGoogleUsernameAlreadyExists() {
            GoogleRegisterRequest request = new GoogleRegisterRequest();
            request.setIdToken("mock.google.id.token");
            request.setUsername("taken_username");

            Jwt jwt = mock(Jwt.class);
            when(jwt.getClaimAsString("email")).thenReturn("newgoogleuser@example.com");
            when(googleJwtDecoder.decode("mock.google.id.token")).thenReturn(jwt);

            when(userRepository.existsByEmail("newgoogleuser@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("taken_username")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerGoogleUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Username già in uso");

            verify(userRepository, never()).save(any());
        }
    }
}