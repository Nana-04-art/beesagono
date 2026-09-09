package com.beesagono.backend.controller;

import com.beesagono.backend.dto.auth.CreateAdminRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    if (SecurityContextHolder.getContext().getAuthentication() != null) {
                        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    }
                    return null;
                }
            });
        }
    }

    @BeforeEach
    void setUp() {
        UserDetailsImpl principal = new UserDetailsImpl(
                "admin-1",
                "admin",
                "admin@example.com",
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/admin/users - Success")
    void getUsers_Success() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id("user-1")
                .username("testuser")
                .email("test@example.com")
                .build();
        Page<UserResponse> page = new PageImpl<>(List.of(userResponse));

        when(adminService.getUsers(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("user-1"))
                .andExpect(jsonPath("$.content[0].username").value("testuser"));

        verify(adminService, times(1)).getUsers(eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/admin/users - With Search Parameter Success")
    void getUsers_WithSearch_Success() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id("user-1")
                .username("searchedUser")
                .email("search@example.com")
                .build();
        Page<UserResponse> page = new PageImpl<>(List.of(userResponse));

        when(adminService.getUsers(eq("searchedUser"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users")
                .param("search", "searchedUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("searchedUser"));

        verify(adminService, times(1)).getUsers(eq("searchedUser"), any(Pageable.class));
    }

    @Test
    @DisplayName("POST /api/admin/users/admin - Success")
    void createAdmin_Success() throws Exception {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("newadmin");
        request.setEmail("newadmin@example.com");
        request.setPassword("password123");

        UserResponse response = UserResponse.builder()
                .id("admin-2")
                .username("newadmin")
                .email("newadmin@example.com")
                .roles(Set.of("ROLE_ADMIN", "ROLE_USER"))
                .build();

        when(adminService.createAdmin(any(CreateAdminRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/users/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("admin-2"))
                .andExpect(jsonPath("$.username").value("newadmin"))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));

        verify(adminService, times(1)).createAdmin(any(CreateAdminRequest.class));
    }
}