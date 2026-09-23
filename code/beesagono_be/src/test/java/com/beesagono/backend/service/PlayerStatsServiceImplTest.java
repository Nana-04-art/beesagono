package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceImplTest {

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @InjectMocks
    private PlayerStatsServiceImpl playerStatsService;

    private String userId;
    private PlayerStats existingStats;

    @BeforeEach
    void setUp() {
        userId = "user-uuid-123";

        existingStats = PlayerStats.builder()
                .userId(userId)
                .gamesPlayed(10)
                .gamesCompleted(8)
                .currentStreak(2)
                .maxStreak(5)
                .longestWordFound("ALBERO")
                .totalScoreEarned(500)
                .build();
    }

    // --- getPlayerStats Tests ---

    @Nested
    @DisplayName("getPlayerStats Tests")
    class GetPlayerStatsTests {

        @Test
        @DisplayName("Should return stats successfully with dynamically recalculated streak")
        void shouldGetPlayerStatsSuccessfullyWithCalculatedStreak() {
            LocalDate today = LocalDate.now();
            List<LocalDate> playedDates = List.of(today, today.minusDays(1), today.minusDays(2));

            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));
            when(gameSessionRepository.findDistinctPlayedPuzzleDatesByUserId(userId)).thenReturn(playedDates);
            when(playerStatsRepository.save(any(PlayerStats.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PlayerStatsResponse response = playerStatsService.getPlayerStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getGamesPlayed()).isEqualTo(10);
            assertThat(response.getGamesCompleted()).isEqualTo(8);
            assertThat(response.getCurrentStreak()).isEqualTo(3);
            assertThat(response.getMaxStreak()).isEqualTo(5);
            assertThat(response.getAverageScorePerGame()).isEqualTo(50.0);
            assertThat(response.getCompletionRate()).isEqualTo(80.0);

            verify(playerStatsRepository, times(1)).save(any(PlayerStats.class));
        }

        @Test
        @DisplayName("Should create initial default stats for new user")
        void shouldCreateInitialStatsWhenUserNotFoundOnGetPlayerStats() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.empty());
            when(gameSessionRepository.findDistinctPlayedPuzzleDatesByUserId(userId))
                    .thenReturn(Collections.emptyList());
            when(playerStatsRepository.save(any(PlayerStats.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PlayerStatsResponse response = playerStatsService.getPlayerStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getGamesPlayed()).isZero();
            assertThat(response.getGamesCompleted()).isZero();
            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getMaxStreak()).isZero();
            assertThat(response.getAverageScorePerGame()).isEqualTo(0.0);
            assertThat(response.getCompletionRate()).isEqualTo(0.0);

            verify(playerStatsRepository, times(1)).save(any(PlayerStats.class));
        }

        @Test
        @DisplayName("Should reset streak to 0 if last played date is older than yesterday")
        void shouldResetStreakToZeroWhenGapInDates() {
            LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
            List<LocalDate> playedDates = List.of(threeDaysAgo, threeDaysAgo.minusDays(1));

            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));
            when(gameSessionRepository.findDistinctPlayedPuzzleDatesByUserId(userId)).thenReturn(playedDates);
            when(playerStatsRepository.save(any(PlayerStats.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PlayerStatsResponse response = playerStatsService.getPlayerStats(userId);

            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getMaxStreak()).isEqualTo(5);
        }
    }

    // --- updatePlayerStatsAfterGame Tests ---

    @Nested
    @DisplayName("updatePlayerStatsAfterGame Tests")
    class UpdatePlayerStatsAfterGameTests {

        @Test
        @DisplayName("Should update score, game counters, longest word and max streak successfully")
        void shouldUpdatePlayerStatsAfterGameSuccessfully() {
            LocalDate today = LocalDate.now();
            List<LocalDate> playedDates = List.of(today, today.minusDays(1), today.minusDays(2), today.minusDays(3),
                    today.minusDays(4), today.minusDays(5));

            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));
            when(gameSessionRepository.findDistinctPlayedPuzzleDatesByUserId(userId)).thenReturn(playedDates);

            playerStatsService.updatePlayerStatsAfterGame(userId, 150, "MIELEGRAMMA", true);

            ArgumentCaptor<PlayerStats> captor = ArgumentCaptor.forClass(PlayerStats.class);
            verify(playerStatsRepository, times(1)).save(captor.capture());

            PlayerStats savedStats = captor.getValue();
            assertThat(savedStats.getGamesPlayed()).isEqualTo(11);
            assertThat(savedStats.getGamesCompleted()).isEqualTo(9);
            assertThat(savedStats.getTotalScoreEarned()).isEqualTo(650);
            assertThat(savedStats.getLongestWordFound()).isEqualTo("MIELEGRAMMA");
            assertThat(savedStats.getCurrentStreak()).isEqualTo(6);
            assertThat(savedStats.getMaxStreak()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should keep existing longest word if new word is shorter")
        void shouldKeepExistingLongestWordIfNewWordIsShorter() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));
            when(gameSessionRepository.findDistinctPlayedPuzzleDatesByUserId(userId))
                    .thenReturn(Collections.emptyList());

            playerStatsService.updatePlayerStatsAfterGame(userId, 20, "CASA", false);

            ArgumentCaptor<PlayerStats> captor = ArgumentCaptor.forClass(PlayerStats.class);
            verify(playerStatsRepository, times(1)).save(captor.capture());

            PlayerStats savedStats = captor.getValue();
            assertThat(savedStats.getLongestWordFound()).isEqualTo("ALBERO");
            assertThat(savedStats.getGamesPlayed()).isEqualTo(11);
            assertThat(savedStats.getGamesCompleted()).isEqualTo(8);
        }
    }
}