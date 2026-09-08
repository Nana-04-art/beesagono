package com.beesagono.backend.repository;

import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuzzleOuterLetterRepository extends JpaRepository<PuzzleOuterLetter, PuzzleOuterLetterId> {
}