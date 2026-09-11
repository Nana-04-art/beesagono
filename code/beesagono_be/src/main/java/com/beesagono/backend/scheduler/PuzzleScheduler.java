package com.beesagono.backend.scheduler;

import com.beesagono.backend.service.PuzzleGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PuzzleScheduler {

    private final PuzzleGeneratorService puzzleGeneratorService;

    /**
     * DEPLOY: Runs the job every day at exactly midnight (00:00:00).
     * Generates the puzzle for TOMORROW (D+1) 24 hours in advance.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailyPuzzleJob() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        log.info(">>> [SCHEDULER] Generazione automatica puzzle per domani ({})", tomorrow);
        checkAndGeneratePuzzleForDate(tomorrow);
    }

    /**
     * DEVELOPMENT: Triggers AUTOMATICALLY every time the server starts.
     * Ensures that puzzles for BOTH today (D) and tomorrow (D+1) exist in the DB.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        log.info(">>> Server avviato: verifica presenza puzzle per oggi ({}) e domani ({})...", today, tomorrow);
        checkAndGeneratePuzzleForDate(today);
        checkAndGeneratePuzzleForDate(tomorrow);
    }

    private void checkAndGeneratePuzzleForDate(LocalDate date) {
        try {
            puzzleGeneratorService.generateAndSavePuzzleForDate(date);
        } catch (Exception e) {
            log.error(">>> Errore durante la generazione del puzzle per il {}: {}", date, e.getMessage(), e);
        }
    }
}