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
         * Trova tutte le parole che:
         * 1. Contengono la lettera centrale: (letter_mask & centerBit) = centerBit
         * 2. Sono composte SOLO da sottoinsiemi delle lettere del puzzle: (letter_mask
         * | puzzleMask) = puzzleMask
         */
        @Query(value = "SELECT * FROM dictionary_words dw " +
                        "WHERE (dw.letter_mask & :centerBit) = :centerBit " +
                        "AND (dw.letter_mask | :puzzleMask) = :puzzleMask", nativeQuery = true)
        List<DictionaryWord> findValidWordsForPuzzle(@Param("centerBit") int centerBit,
                        @Param("puzzleMask") int puzzleMask);
}