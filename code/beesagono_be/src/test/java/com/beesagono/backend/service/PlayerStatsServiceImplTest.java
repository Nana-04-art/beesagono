package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatsServiceImplTest {

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @InjectMocks
    private PlayerStatsServiceImpl playerStatsService;

    @Captor
    private ArgumentCaptor<PlayerStats> statsCaptor;

    private String userId;
    private PlayerStats existingStats;

    @BeforeEach
    void setUp() {
        userId = "user-123";

        existingStats = PlayerStats.builder()
                .userId(userId)
                .gamesPlayed(10)
                .gamesCompleted(8)
                .currentStreak(3)
                .maxStreak(5)
                .totalScoreEarned(500)
                .longestWordFound("ALBERO")
                .build();
    }

    // --- getPlayerStats Tests ---

    @Nested
    @DisplayName("getPlayerStats Tests")
    class GetPlayerStatsTests {

        @Test
        @DisplayName("Should return mapped response when PlayerStats exist")
        void getPlayerStats_ExistingUser_ReturnsMappedResponse() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));

            PlayerStatsResponse response = playerStatsService.getPlayerStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getGamesPlayed()).isEqualTo(10);
            assertThat(response.getGamesCompleted()).isEqualTo(8);
            assertThat(response.getCurrentStreak()).isEqualTo(3);
            assertThat(response.getMaxStreak()).isEqualTo(5);
            assertThat(response.getTotalScoreEarned()).isEqualTo(500);
            assertThat(response.getLongestWordFound()).isEqualTo("ALBERO");
            // 500 / 10 = 50.0
            assertThat(response.getAverageScorePerGame()).isEqualTo(50.0);
            // (8 / 10) * 100 = 80.0
            assertThat(response.getCompletionRate()).isEqualTo(80.0);

            verify(playerStatsRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("Should return default empty stats when PlayerStats do not exist in DB")
        void getPlayerStats_NewUser_ReturnsDefaultStats() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.empty());

            PlayerStatsResponse response = playerStatsService.getPlayerStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getGamesPlayed()).isZero();
            assertThat(response.getGamesCompleted()).isZero();
            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getMaxStreak()).isZero();
            assertThat(response.getTotalScoreEarned()).isZero();
            assertThat(response.getLongestWordFound()).isNull();
            assertThat(response.getAverageScorePerGame()).isEqualTo(0.0);
            assertThat(response.getCompletionRate()).isEqualTo(0.0);

            verify(playerStatsRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("Should handle null fields gracefully without NullPointerException")
        void getPlayerStats_NullFields_HandlesDefaultsCorrectly() {
            PlayerStats statsWithNulls = PlayerStats.builder()
                    .userId(userId)
                    .gamesPlayed(null)
                    .gamesCompleted(null)
                    .currentStreak(null)
                    .maxStreak(null)
                    .totalScoreEarned(null)
                    .longestWordFound(null)
                    .build();

            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(statsWithNulls));

            PlayerStatsResponse response = playerStatsService.getPlayerStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getGamesPlayed()).isZero();
            assertThat(response.getGamesCompleted()).isZero();
            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getMaxStreak()).isZero();
            assertThat(response.getTotalScoreEarned()).isZero();
            assertThat(response.getAverageScorePerGame()).isEqualTo(0.0);
            assertThat(response.getCompletionRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should correctly round double values for averageScore and completionRate")
        void getPlayerStats_RoundsDoubleValuesToTwoDecimals() {
            PlayerStats statsForRounding = PlayerStats.builder()
                    .userId(userId)
                    .gamesPlayed(3)
                    .gamesCompleted(1)
                    .totalScoreEarned(100)
                    .build();

            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(statsForRounding));

            PlayerStatsResponse response = playerStatsService.getPlayerStats(userId);

            // 100 / 3 = 33.3333... -> 33.33
            assertThat(response.getAverageScorePerGame()).isEqualTo(33.33);
            // (1 / 3) * 100 = 33.3333... -> 33.33
            assertThat(response.getCompletionRate()).isEqualTo(33.33);
        }
    }

    // --- updatePlayerStatsAfterGame Tests ---

    @Nested
    @DisplayName("updatePlayerStatsAfterGame Tests")
    class UpdatePlayerStatsAfterGameTests {

        @Test
        @DisplayName("Should update existing player stats after completed game with longer word")
        void updatePlayerStats_ExistingUser_CompletedGameAndNewLongestWord() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));

            playerStatsService.updatePlayerStatsAfterGame(userId, 150, "CANCELLERIA", true);

            verify(playerStatsRepository).save(statsCaptor.capture());
            PlayerStats savedStats = statsCaptor.getValue();

            assertThat(savedStats.getGamesPlayed()).isEqualTo(11); // 10 + 1
            assertThat(savedStats.getGamesCompleted()).isEqualTo(9); // 8 + 1
            assertThat(savedStats.getTotalScoreEarned()).isEqualTo(650); // 500 + 150
            assertThat(savedStats.getLongestWordFound()).isEqualTo("CANCELLERIA"); // "CANCELLERIA" > "ALBERO"
        }

        @Test
        @DisplayName("Should keep old longest word if new word is shorter or equal in length")
        void updatePlayerStats_ExistingUser_WordShorterOrEqual_KeepCurrentLongest() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));

            playerStatsService.updatePlayerStatsAfterGame(userId, 50, "CASA", false);

            verify(playerStatsRepository).save(statsCaptor.capture());
            PlayerStats savedStats = statsCaptor.getValue();

            assertThat(savedStats.getGamesPlayed()).isEqualTo(11);
            assertThat(savedStats.getGamesCompleted()).isEqualTo(8); // Non completata, rimane 8
            assertThat(savedStats.getTotalScoreEarned()).isEqualTo(550);
            assertThat(savedStats.getLongestWordFound()).isEqualTo("ALBERO"); // "CASA" < "ALBERO"
        }

        @Test
        @DisplayName("Should initialize and save new stats if user record does not exist")
        void updatePlayerStats_NewUser_CreatesAndSavesInitialStats() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.empty());

            playerStatsService.updatePlayerStatsAfterGame(userId, 80, "BEESAGONO", true);

            verify(playerStatsRepository).save(statsCaptor.capture());
            PlayerStats savedStats = statsCaptor.getValue();

            assertThat(savedStats.getUserId()).isEqualTo(userId);
            assertThat(savedStats.getGamesPlayed()).isEqualTo(1);
            assertThat(savedStats.getGamesCompleted()).isEqualTo(1);
            assertThat(savedStats.getTotalScoreEarned()).isEqualTo(80);
            assertThat(savedStats.getLongestWordFound()).isEqualTo("BEESAGONO");
        }

        @Test
        @DisplayName("Should handle null bestWordFound without throwing exception")
        void updatePlayerStats_NullBestWord_DoesNotUpdateLongestWord() {
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(existingStats));

            playerStatsService.updatePlayerStatsAfterGame(userId, 30, null, false);

            verify(playerStatsRepository).save(statsCaptor.capture());
            PlayerStats savedStats = statsCaptor.getValue();

            assertThat(savedStats.getGamesPlayed()).isEqualTo(11);
            assertThat(savedStats.getLongestWordFound()).isEqualTo("ALBERO");
        }
    }
}