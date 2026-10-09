package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.id.PuzzleWordId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PuzzleWordRepository extends JpaRepository<PuzzleWord, PuzzleWordId> {

    Optional<PuzzleWord> findByIdPuzzleIdAndIdWord(String puzzleId, String word);

    int countByIdPuzzleId(String puzzleId);

    void deleteByIdWord(String word);
}