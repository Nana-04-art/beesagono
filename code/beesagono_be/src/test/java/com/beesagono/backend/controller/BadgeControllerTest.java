package com.beesagono.backend.controller;

import com.beesagono.backend.dto.badge.BadgeResponse;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.JwtAuthenticationFilter;
import com.beesagono.backend.security.JwtUtils;
import com.beesagono.backend.security.TokenBlacklist;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.BadgeService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BadgeController.class)
@AutoConfigureMockMvc(addFilters = false)
class BadgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BadgeService badgeService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private TokenBlacklist tokenBlacklist;

    private UserDetailsImpl principal;

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
        principal = new UserDetailsImpl(
                "user-1",
                "testuser",
                "user@example.com",
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // --- getUserBadges ---

    @Test
    @DisplayName("GET /api/badges - Success")
    void getUserBadges_Success() throws Exception {
        BadgeResponse badgeResponse = new BadgeResponse();
        List<BadgeResponse> badgesList = List.of(badgeResponse);

        when(badgeService.getUserBadges(anyString())).thenReturn(badgesList);

        mockMvc.perform(get("/api/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(badgeService, times(1)).evaluateAndAwardBadges("user-1");
        verify(badgeService, times(1)).getUserBadges("user-1");
    }

    // --- evaluateBadges ---

    @Test
    @DisplayName("POST /api/badges/evaluate - Success (No Content)")
    void evaluateBadges_Success() throws Exception {
        mockMvc.perform(post("/api/badges/evaluate"))
                .andExpect(status().isNoContent());

        verify(badgeService, times(1)).evaluateAndAwardBadges("user-1");
    }
}