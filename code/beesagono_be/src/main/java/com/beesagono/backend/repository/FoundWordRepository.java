package com.beesagono.backend.repository;

import com.beesagono.backend.entity.FoundWord;
import com.beesagono.backend.entity.id.FoundWordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface FoundWordRepository extends JpaRepository<FoundWord, FoundWordId> {

    List<FoundWord> findByIdSessionId(String sessionId);

    boolean existsByIdSessionIdAndIdWord(String sessionId, String word);

    // Mapping for Badge calculation
    long countBySessionUserId(String userId);

    @Query("SELECT COUNT(fw) FROM FoundWord fw WHERE fw.session.user.id = :userId AND fw.isMielegramma = true")
    long countPangramsByUserId(@Param("userId") String userId);

    // Date of the user's very first word found
    @Query("SELECT MIN(fw.session.startTime) FROM FoundWord fw WHERE fw.session.user.id = :userId")
    Optional<Date> findFirstWordDateByUserId(@Param("userId") String userId);

    // Date of the Nth word found (e.g., for the 100th, 500th, 1000th word)
    @Query(value = "SELECT s.start_time FROM found_words fw " +
            "JOIN game_sessions s ON fw.session_id = s.id " +
            "WHERE s.user_id = :userId " +
            "ORDER BY s.start_time ASC LIMIT 1 OFFSET :offset", nativeQuery = true)
    Optional<Date> findNthWordDateByUserId(@Param("userId") String userId, @Param("offset") int offset);

    // Date of the first pangram found
    @Query("SELECT MIN(fw.session.startTime) FROM FoundWord fw WHERE fw.session.user.id = :userId AND fw.isMielegramma = true")
    Optional<Date> findFirstPangramDateByUserId(@Param("userId") String userId);

    // Date of the Nth pangram found
    @Query(value = "SELECT s.start_time FROM found_words fw " +
            "JOIN game_sessions s ON fw.session_id = s.id " +
            "WHERE s.user_id = :userId AND fw.is_mielegramma = true " +
            "ORDER BY s.start_time ASC LIMIT 1 OFFSET :offset", nativeQuery = true)
    Optional<Date> findNthPangramDateByUserId(@Param("userId") String userId, @Param("offset") int offset);
}