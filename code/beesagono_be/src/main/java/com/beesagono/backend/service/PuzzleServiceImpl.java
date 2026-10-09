package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.mapper.DailyPuzzleMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PuzzleServiceImpl implements PuzzleService {

    private final DailyPuzzleRepository dailyPuzzleRepository;
    private final PuzzleGeneratorService puzzleGeneratorService;
    private final DailyPuzzleMapper dailyPuzzleMapper;

    @Override
    @Transactional
    public DailyPuzzleResponse getTodayPuzzle() {
        LocalDate today = LocalDate.now();

        if (!dailyPuzzleRepository.existsByPuzzleDate(today)) {
            try {
                puzzleGeneratorService.generateAndSavePuzzleForDate(today);
            } catch (DataIntegrityViolationException e) {
                // If another concurrent request has just created the puzzle, ignore the
                // duplicate
            }
        }

        DailyPuzzle puzzle = dailyPuzzleRepository.findByPuzzleDate(today)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Puzzle del giorno non trovato"));

        return dailyPuzzleMapper.toDailyPuzzleResponse(puzzle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getValidWordsByDate(LocalDate date) {
        DailyPuzzle puzzle = dailyPuzzleRepository.findByPuzzleDate(date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Puzzle non trovato per la data: " + date));

        return puzzle.getPuzzleWords().stream()
                .map(pw -> pw.getId().getWord())
                .toList();
    }
}