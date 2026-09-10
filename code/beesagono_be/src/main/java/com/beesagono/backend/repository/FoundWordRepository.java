package com.beesagono.backend.repository;

import com.beesagono.backend.entity.FoundWord;
import com.beesagono.backend.entity.id.FoundWordId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoundWordRepository extends JpaRepository<FoundWord, FoundWordId> {

    List<FoundWord> findByIdSessionId(String sessionId);

    boolean existsByIdSessionIdAndIdWord(String sessionId, String word);
}