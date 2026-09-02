package com.beesagono.backend.service;

import com.beesagono.backend.dto.dictionary.*;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.mapper.DictionaryWordMapper;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.specification.DictionaryWordSpecification;

import lombok.RequiredArgsConstructor;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final DictionaryWordRepository dictionaryWordRepository;
    private final DictionaryWordMapper dictionaryWordMapper;

    @Override
    @Transactional
    public DictionaryWordResponse addSingleWord(AddWordRequest request, User adminUser) {
        String cleanWord = sanitizeWord(request.getWord());

        if (cleanWord.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La parola deve contenere almeno 4 lettere.");
        }

        if (dictionaryWordRepository.existsById(cleanWord)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La parola è già presente nel dizionario.");
        }

        int uniqueLetters = (int) cleanWord.chars().distinct().count();

        if (uniqueLetters > 7) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La parola contiene più di 7 lettere uniche e non può essere usata nel gioco.");
        }

        boolean isPangram = request.getIsCandidatePangram() != null
                ? request.getIsCandidatePangram()
                : uniqueLetters == 7;

        DictionaryWord entity = DictionaryWord.builder()
                .word(cleanWord)
                .wordLength(cleanWord.length())
                .uniqueLettersCount(uniqueLetters)
                .isCandidatePangram(isPangram)
                .addedByUser(adminUser)
                .addedAt(new java.util.Date())
                .build();

        DictionaryWord saved = dictionaryWordRepository.save(entity);
        return dictionaryWordMapper.toDictionaryWordResponse(saved);
    }

    @Override
    @Transactional
    public BatchUploadResponse addBatchWords(BatchAddWordRequest request, User adminUser) {
        return processAndSaveWords(request.getWords(), adminUser);
    }

    @Override
    @Transactional
    public BatchUploadResponse uploadWordsFromFile(MultipartFile file, User adminUser) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il file inviato è vuoto.");
        }

        List<String> rawWords = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("[,\\s]+");
                for (String token : tokens) {
                    if (!token.isBlank()) {
                        rawWords.add(token);
                    }
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante la lettura del file: " + e.getMessage());
        }

        return processAndSaveWords(rawWords, adminUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DictionaryWordResponse> getWords(DictionaryFilterRequest filterRequest, Pageable pageable) {
        Specification<DictionaryWord> spec = DictionaryWordSpecification.buildSpecification(filterRequest);
        return dictionaryWordRepository.findAll(spec, pageable)
                .map(dictionaryWordMapper::toDictionaryWordResponse);
    }

    private BatchUploadResponse processAndSaveWords(List<String> rawWords, User adminUser) {
        Set<String> cleanedWords = rawWords.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeWord)
                .filter(w -> w.length() >= 4)
                .filter(w -> w.chars().distinct().count() <= 7)
                .collect(Collectors.toSet());

        if (cleanedWords.isEmpty()) {
            return BatchUploadResponse.builder()
                    .totalProcessed(rawWords.size())
                    .addedCount(0)
                    .skippedCount(rawWords.size())
                    .message("Nessuna parola valida trovata (richieste min 4 lettere e max 7 lettere distinte).")
                    .build();
        }

        Set<String> existingWords = dictionaryWordRepository.findAllById(cleanedWords).stream()
                .map(DictionaryWord::getWord)
                .collect(Collectors.toSet());

        List<DictionaryWord> newEntities = cleanedWords.stream()
                .filter(w -> !existingWords.contains(w))
                .map(w -> {
                    int uniqueCount = (int) w.chars().distinct().count();
                    return DictionaryWord.builder()
                            .word(w)
                            .uniqueLettersCount(uniqueCount)
                            .isCandidatePangram(uniqueCount == 7)
                            .addedByUser(adminUser)
                            .build();
                })
                .collect(Collectors.toList());

        dictionaryWordRepository.saveAll(newEntities);

        int added = newEntities.size();
        int skipped = rawWords.size() - added;

        return BatchUploadResponse.builder()
                .totalProcessed(rawWords.size())
                .addedCount(added)
                .skippedCount(skipped)
                .message(String.format("Inserite %d parole. Ignorate %d (duplicate, corte o con > 7 lettere uniche).",
                        added, skipped))
                .build();
    }

    /**
     * Removes accents, spaces, and non-A-Z characters, and converts to uppercase
     */
    private String sanitizeWord(String input) {
        String normalized = Normalizer.normalize(input.trim().toUpperCase(Locale.ITALIAN), Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("[^A-Z]", "");
    }
}