package com.beesagono.backend.repository;

import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStat;
import com.beesagono.backend.entity.InvalidWordAttempt;
import com.beesagono.backend.enums.ErrorTypeCode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvalidWordAttemptRepository extends JpaRepository<InvalidWordAttempt, String> {

    List<InvalidWordAttempt> findBySessionId(String sessionId);

    /**
     * Groups and counts the frequency of each invalid word for a specific error
     * type
     */
    @Query("SELECT i.attemptedWord AS attemptedWord, COUNT(i) AS attemptCount " +
            "FROM InvalidWordAttempt i " +
            "WHERE i.errorReason = :errorReason " +
            "GROUP BY i.attemptedWord")
    List<InvalidWordAttemptStat> findByErrorReason(@Param("errorReason") ErrorTypeCode errorReason);

    @Query("SELECT DISTINCT i.attemptedWord FROM InvalidWordAttempt i WHERE i.session.id = :sessionId")
    List<String> findDistinctAttemptedWordsBySessionId(@Param("sessionId") String sessionId);
}