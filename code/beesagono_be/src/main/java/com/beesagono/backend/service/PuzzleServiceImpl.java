package com.beesagono.backend.service;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.mapper.DailyPuzzleMapper;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
            puzzleGeneratorService.generateAndSavePuzzleForDate(today);
        }

        DailyPuzzle puzzle = dailyPuzzleRepository.findByPuzzleDate(today)
                .orElseThrow(() -> new RuntimeException("Puzzle del giorno non trovato"));

        return dailyPuzzleMapper.toDailyPuzzleResponse(puzzle);
    }
}