package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.dto.puzzle.WordSubmissionResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.mapper.DailyPuzzleMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PuzzleServiceImpl implements PuzzleService {

    private final DailyPuzzleRepository dailyPuzzleRepository;
    private final PuzzleWordRepository puzzleWordRepository;
    private final GameSessionRepository gameSessionRepository;
    private final PuzzleGeneratorService puzzleGeneratorService;
    private final DailyPuzzleMapper dailyPuzzleMapper;

    @Override
    @Transactional
    public DailyPuzzleResponse getTodayPuzzle() {
        LocalDate today = LocalDate.now();

        if (!dailyPuzzleRepository.existsByPuzzleDate(today)) {
            puzzleGeneratorService.generateAndSavePuzzleForDate(today);
        }

        DailyPuzzle puzzle = dailyPuzzleRepository.findByPuzzleDate(today)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Puzzle del giorno non trovato"));

        return dailyPuzzleMapper.toDailyPuzzleResponse(puzzle);
    }

    @Override
    @Transactional(readOnly = true)
    public WordSubmissionResponse validateAndScoreWord(String userId, String puzzleId, String rawWord) {
        // Puzzle existence check
        DailyPuzzle puzzle = dailyPuzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Puzzle non trovato con ID: " + puzzleId));

        // Syntactic input validation
        if (rawWord == null || rawWord.isBlank() || rawWord.trim().length() < 4) {
            return new WordSubmissionResponse(false, rawWord, 0, false, ErrorTypeCode.TOO_SHORT,
                    "La parola deve contenere almeno 4 lettere.");
        }

        String word = rawWord.trim().toUpperCase();

        // Duplicate word check in user session
        if (gameSessionRepository.existsByUserIdAndPuzzleIdAndFoundWordsContaining(userId, puzzleId, word)) {
            return new WordSubmissionResponse(false, word, 0, false, ErrorTypeCode.ALREADY_FOUND,
                    "Hai già trovato questa parola.");
        }

        // Center letter check
        if (!word.contains(puzzle.getCenterLetter())) {
            return new WordSubmissionResponse(false, word, 0, false, ErrorTypeCode.MISSING_CENTER,
                    "La parola non contiene la lettera centrale obbligatoria.");
        }

        // Dictionary lookup and score calculation
        return puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzleId, word)
                .map(pw -> {
                    boolean isMiele = Boolean.TRUE.equals(pw.getIsMielegramma());
                    int basePoints = word.length() == 4 ? 1 : word.length();
                    int score = isMiele ? basePoints + 7 : basePoints;

                    return new WordSubmissionResponse(true, word, score, isMiele, null, null);
                })
                .orElseGet(() -> new WordSubmissionResponse(false, word, 0, false, ErrorTypeCode.NOT_IN_DICTIONARY,
                        "La parola non è presente nel dizionario ufficiale."));
    }
}