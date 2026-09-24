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
     * Cron expression: "seconds minutes hours day-of-month month
     * day-of-week"
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailyPuzzleJob() {
        checkAndGeneratePuzzleForDate(LocalDate.now());
    }

    /**
     * DEVELOPMENT: Triggers AUTOMATICALLY every time you start the
     * server.
     * If you launch the app at 11:00 AM and today's puzzle is missing, it creates
     * it on the fly.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStart() {
        log.info(">>> Server avviato: verifica presenza puzzle per la data odierna...");
        checkAndGeneratePuzzleForDate(LocalDate.now());
    }

    private void checkAndGeneratePuzzleForDate(LocalDate date) {
        try {
            puzzleGeneratorService.generateAndSavePuzzleForDate(date);
        } catch (Exception e) {
            log.error(">>> Errore durante la generazione del puzzle per il {}: {}", date, e.getMessage(), e);
        }
    }
}