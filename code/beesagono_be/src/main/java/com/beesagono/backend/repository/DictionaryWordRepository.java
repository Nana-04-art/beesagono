package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DictionaryWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DictionaryWordRepository
                extends JpaRepository<DictionaryWord, String>, JpaSpecificationExecutor<DictionaryWord> {

        Optional<DictionaryWord> findByWord(String word);

        Boolean existsByWord(String word);

        @Query("SELECT dw.word FROM DictionaryWord dw WHERE dw.isCandidatePangram = true")
        List<String> findCandidatePangrams();

        /**
         * Find all words that:
         * Contain the center letter: (letter_mask & centerBit) = centerBit
         * Are composed ONLY of subsets of the puzzle letters: (letter_mask |
         * puzzleMask) = puzzleMask
         */
        @Query("SELECT dw FROM DictionaryWord dw " +
                        "WHERE bitand(dw.letterMask, cast(:centerBit as integer)) = cast(:centerBit as integer) " +
                        "AND bitor(dw.letterMask, cast(:puzzleMask as integer)) = cast(:puzzleMask as integer)")
        List<DictionaryWord> findValidWordsForPuzzle(@Param("centerBit") int centerBit,
                        @Param("puzzleMask") int puzzleMask);
}