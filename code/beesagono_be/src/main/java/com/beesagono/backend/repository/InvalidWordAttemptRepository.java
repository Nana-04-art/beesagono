package com.beesagono.backend.repository;

import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStat;
import com.beesagono.backend.entity.InvalidWordAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvalidWordAttemptRepository extends JpaRepository<InvalidWordAttempt, String> {

    List<InvalidWordAttempt> findBySessionId(String sessionId);

    // Spring Data JPA automatically infers the GROUP BY and COUNT based
    // on the projection
    List<InvalidWordAttemptStat> findByErrorReason(String errorReason);
}