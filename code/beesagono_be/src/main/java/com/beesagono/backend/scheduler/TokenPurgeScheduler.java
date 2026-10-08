package com.beesagono.backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.beesagono.backend.security.TokenBlacklist;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenPurgeScheduler {

    private final TokenBlacklist tokenBlacklist;

    @Scheduled(fixedDelay = 3600000)
    public void purgeBlacklist() {
        log.info("Avvio pulizia programmata dei token JWT scaduti dalla blacklist...");
        tokenBlacklist.purgeExpiredTokens();
        log.info("Pulizia token completata con successo.");
    }
}