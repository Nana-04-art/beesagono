package com.beesagono.backend.scheduler;

import com.beesagono.backend.security.TokenBlacklist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenPurgeSchedulerTest {

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private TokenPurgeScheduler tokenPurgeScheduler;

    @Test
    @DisplayName("purgeBlacklist - Invokes token blacklist purge")
    void shouldPurgeBlacklist() {
        tokenPurgeScheduler.purgeBlacklist();

        verify(tokenBlacklist, times(1)).purgeExpiredTokens();
    }
}