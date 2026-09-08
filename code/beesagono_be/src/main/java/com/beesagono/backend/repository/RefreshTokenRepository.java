package com.beesagono.backend.repository;

import com.beesagono.backend.entity.RefreshToken;
import com.beesagono.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    int deleteByUser(User user);
}