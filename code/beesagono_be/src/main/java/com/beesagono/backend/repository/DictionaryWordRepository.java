package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DictionaryWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DictionaryWordRepository extends JpaRepository<DictionaryWord, String> {
    Optional<DictionaryWord> findByWord(String word);
    Boolean existsByWord(String word);
}