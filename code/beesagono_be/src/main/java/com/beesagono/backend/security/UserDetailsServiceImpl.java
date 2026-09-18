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
 * Service implementation of {@link UserDetailsService} for loading user
 * credentials
 * and granted authorities from the database by username or email.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Locates a user based on their username or email address.
     *
     * @param usernameOrEmail the identifier to search for
     * @return a fully populated {@link UserDetails} object
     * @throws UsernameNotFoundException if no user matching the identifier is found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utente non trovato con username o email: " + usernameOrEmail));

        return UserDetailsImpl.build(user);
    }
}