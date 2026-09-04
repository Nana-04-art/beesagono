package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.CreateAdminRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import com.beesagono.backend.entity.Role;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.enums.RoleName;
import com.beesagono.backend.mapper.UserMapper;
import com.beesagono.backend.repository.RoleRepository;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    @DisplayName("createAdmin - Success")
    void createAdmin_Success() {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("adminuser");
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        Role adminRole = Role.builder().id("role-admin").name(RoleName.ROLE_ADMIN).build();
        Role userRole = Role.builder().id("role-user").name(RoleName.ROLE_USER).build();

        User savedUser = User.builder()
                .id("user-admin-1")
                .username("adminuser")
                .email("admin@example.com")
                .userRoles(new ArrayList<>())
                .build();

        UserResponse expectedResponse = UserResponse.builder()
                .id("user-admin-1")
                .username("adminuser")
                .email("admin@example.com")
                .roles(Set.of("ROLE_ADMIN", "ROLE_USER"))
                .build();

        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(userMapper.toUserResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse response = adminService.createAdmin(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("user-admin-1");
        assertThat(response.getUsername()).isEqualTo("adminuser");

        verify(userRoleRepository, times(2)).save(any());
        verify(userRepository, times(1)).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("createAdmin - Username Conflict Throws 409")
    void createAdmin_UsernameConflict() {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("existingAdmin");

        when(userRepository.existsByUsername("existingAdmin")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username già in uso")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("createAdmin - Email Conflict Throws 409")
    void createAdmin_EmailConflict() {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("newAdmin");
        request.setEmail("existing@example.com");

        when(userRepository.existsByUsername("newAdmin")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email già in uso")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("createAdmin - Role ADMIN Missing Throws 500")
    void createAdmin_RoleAdminNotFound() {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("adminuser");
        request.setEmail("admin@example.com");

        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ruolo ADMIN non trovato")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("getUsers - Without Search Query Success")
    void getUsers_WithoutSearch_Success() {
        Pageable pageable = Pageable.unpaged();
        User user = User.builder().id("u1").username("user1").build();
        UserResponse responseDto = UserResponse.builder().id("u1").username("user1").build();

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toUserResponse(user)).thenReturn(responseDto);

        Page<UserResponse> result = adminService.getUsers(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("user1");
        verify(userRepository, times(1)).findAll(pageable);
        verify(userRepository, never()).findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(any(), any(),
                any());
    }

    @Test
    @DisplayName("getUsers - With Search Query Success")
    void getUsers_WithSearch_Success() {
        Pageable pageable = Pageable.unpaged();
        String searchKey = "john";

        User user = User.builder().id("u2").username("john_doe").build();
        UserResponse responseDto = UserResponse.builder().id("u2").username("john_doe").build();

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchKey, searchKey,
                pageable))
                .thenReturn(userPage);
        when(userMapper.toUserResponse(user)).thenReturn(responseDto);

        Page<UserResponse> result = adminService.getUsers(searchKey, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("john_doe");
        verify(userRepository, times(1)).findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(searchKey,
                searchKey, pageable);
        verify(userRepository, never()).findAll(pageable);
    }
}