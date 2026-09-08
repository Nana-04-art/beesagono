package com.beesagono.backend.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.beesagono.backend.dto.auth.LoginRequest;
import com.beesagono.backend.dto.auth.LoginResponse;
import com.beesagono.backend.dto.auth.RegisterRequest;
import com.beesagono.backend.dto.auth.RegisterResponse;
import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserRole;
import com.beesagono.backend.entity.id.UserRoleId;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.repository.RoleRepository;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.repository.UserRoleRepository;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenBlacklist tokenBlacklist;
    private final AuthenticationManager authenticationManager;

    private static final List<String> ROLE_PRIORITY = List.of(
            RoleName.ROLE_ADMIN.name(),
            RoleName.ROLE_USER.name());

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email già in uso");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username già in uso");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Ruolo ROLE_USER non trovato"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .userRoles(new ArrayList<>())
                .build();

        User savedUser = userRepository.save(user);

        // Build the EmbeddedId
        UserRoleId userRoleId = new UserRoleId(savedUser.getId(), userRole.getId());

        UserRole userRoleAssociation = UserRole.builder()
                .id(userRoleId)
                .user(savedUser)
                .role(userRole)
                .build();

        userRoleRepository.save(userRoleAssociation);
        savedUser.getUserRoles().add(userRoleAssociation);

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(userRole.getName().name())
                .message("Utente registrato con successo")
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Performs authentication using usernameOrEmail and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        // Set the authentication in the Spring Security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String token = jwtUtils.generateJwtToken(userDetails);
        String primaryRole = extractHighestPriorityRole(userDetails);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .role(primaryRole)
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String pureToken = token.substring(7);
            try {
                if (jwtUtils.validateJwtToken(pureToken)) {
                    Instant expiry = jwtUtils.extractExpiry(pureToken);
                    tokenBlacklist.add(pureToken, expiry);
                }
            } catch (Exception e) {
                log.warn("Invalid or expired token submitted for logout: {}", e.getMessage());
            }
        }
    }

    private String extractHighestPriorityRole(UserDetailsImpl userDetails) {
        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return ROLE_PRIORITY.stream()
                .filter(authorities::contains)
                .findFirst()
                .orElse("ROLE_USER");
    }
}