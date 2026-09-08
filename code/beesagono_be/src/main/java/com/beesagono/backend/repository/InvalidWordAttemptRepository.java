package com.beesagono.backend.repository;

import com.beesagono.backend.entity.InvalidWordAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvalidWordAttemptRepository extends JpaRepository<InvalidWordAttempt, String> {
    List<InvalidWordAttempt> findBySessionId(String sessionId);
}