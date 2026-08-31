package com.beesagono.repository;

import com.beesagono.entity.RefreshToken;
import com.beesagono.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    int deleteByUser(User user);
}