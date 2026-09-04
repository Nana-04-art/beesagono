package com.beesagono.backend.security;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Implements Spring Security's UserDetails to encapsulate information
 * about the authenticated user (ID, username, email, password, and
 * authorities).
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utente non trovato con username o email: " + usernameOrEmail));

        return UserDetailsImpl.build(user);
    }
}