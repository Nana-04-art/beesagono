package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.repository.PlayerSeasonRepository;
import com.beesagono.backend.repository.PlayerStatsRepository;
import com.beesagono.backend.repository.UserRepository;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerSeasonServiceImplTest {

    @Mock
    private PlayerSeasonRepository playerSeasonRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlayerSeasonServiceImpl playerSeasonService;

    @Captor
    private ArgumentCaptor<PlayerSeason> seasonCaptor;

    private User user;
    private PlayerSeason season;
    private PlayerStats stats;
    private String userId;
    private int currentYear;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        currentYear = LocalDate.now().getYear();

        user = User.builder()
                .id(userId)
                .username("testplayer")
                .email("test@example.com")
                .build();

        PlayerSeasonId seasonId = new PlayerSeasonId(userId, currentYear);

        season = PlayerSeason.builder()
                .id(seasonId)
                .user(user)
                .basePoints(30)
                .bonusPoints(10)
                .totalPoints(40)
                .highestTierAchieved("Uovo d'Ape")
                .build();

        stats = PlayerStats.builder()
                .userId(userId)
                .gamesPlayed(10)
                .gamesCompleted(8)
                .currentStreak(3)
                .maxStreak(5)
                .build();
    }

    @Nested
    @DisplayName("getCurrentSeasonStats Tests")
    class GetCurrentSeasonStatsTests {

        @Test
        @DisplayName("Should return existing season stats and player stats successfully")
        void shouldReturnExistingSeasonAndStats() {
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.of(season));
            when(playerStatsRepository.findById(userId))
                    .thenReturn(Optional.of(stats));

            PlayerSeasonResponse response = playerSeasonService.getCurrentSeasonStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getYear()).isEqualTo(currentYear);
            assertThat(response.getBasePoints()).isEqualTo(30);
            assertThat(response.getBonusPoints()).isEqualTo(10);
            assertThat(response.getTotalPoints()).isEqualTo(40);
            assertThat(response.getHighestTierAchieved()).isEqualTo("Uovo d'Ape");
            assertThat(response.getGamesPlayed()).isEqualTo(10);
            assertThat(response.getGamesCompleted()).isEqualTo(8);
            assertThat(response.getCurrentStreak()).isEqualTo(3);
            assertThat(response.getMaxStreak()).isEqualTo(5);

            verify(playerSeasonRepository, times(1)).findByIdUserIdAndIdSeasonYear(userId, currentYear);
            verify(playerStatsRepository, times(1)).findById(userId);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should create initial season if none exists and map stats correctly")
        void shouldCreateInitialSeasonWhenNotPresent() {
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));
            when(playerSeasonRepository.save(any(PlayerSeason.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(playerStatsRepository.findById(userId))
                    .thenReturn(Optional.of(stats));

            PlayerSeasonResponse response = playerSeasonService.getCurrentSeasonStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getYear()).isEqualTo(currentYear);
            assertThat(response.getBasePoints()).isZero();
            assertThat(response.getBonusPoints()).isZero();
            assertThat(response.getTotalPoints()).isZero();
            assertThat(response.getHighestTierAchieved()).isEqualTo("Uovo d'Ape");

            verify(userRepository, times(1)).findById(userId);
            verify(playerSeasonRepository, times(1)).save(any(PlayerSeason.class));
        }

        @Test
        @DisplayName("Should handle missing PlayerStats gracefully by setting default 0 values")
        void shouldHandleMissingPlayerStatsGracefully() {
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.of(season));
            when(playerStatsRepository.findById(userId))
                    .thenReturn(Optional.empty());

            PlayerSeasonResponse response = playerSeasonService.getCurrentSeasonStats(userId);

            assertThat(response).isNotNull();
            assertThat(response.getGamesPlayed()).isZero();
            assertThat(response.getGamesCompleted()).isZero();
            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getMaxStreak()).isZero();
        }
    }

    @Nested
    @DisplayName("updateSeasonProgress Tests")
    class UpdateSeasonProgressTests {

        @Test
        @DisplayName("Should update points and maintain tier when points are under 50")
        void shouldUpdatePointsAndKeepInitialTier() {
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.of(season));

            playerSeasonService.updateSeasonProgress(userId, 5, false);

            verify(playerSeasonRepository).save(seasonCaptor.capture());
            PlayerSeason savedSeason = seasonCaptor.getValue();

            assertThat(savedSeason.getBasePoints()).isEqualTo(35); // 30 + 5
            assertThat(savedSeason.getTotalPoints()).isEqualTo(45); // 35 base + 10 bonus
            assertThat(savedSeason.getHighestTierAchieved()).isEqualTo("Uovo d'Ape");
        }

        @Test
        @DisplayName("Should promote tier to 'Ape Esploratrice' when total points >= 50")
        void shouldPromoteToApeEsploratrice() {
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.of(season));

            playerSeasonService.updateSeasonProgress(userId, 15, false); // 30+15=45 base + 10 bonus = 55 total

            verify(playerSeasonRepository).save(seasonCaptor.capture());
            PlayerSeason savedSeason = seasonCaptor.getValue();

            assertThat(savedSeason.getTotalPoints()).isEqualTo(55);
            assertThat(savedSeason.getHighestTierAchieved()).isEqualTo("Ape Esploratrice");
        }

        @Test
        @DisplayName("Should promote tier to 'Ape Operosa' when total points >= 200")
        void shouldPromoteToApeOperosa() {
            season.setBasePoints(180);
            season.setBonusPoints(10);
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.of(season));

            playerSeasonService.updateSeasonProgress(userId, 15, false); // 195 base + 10 bonus = 205 total

            verify(playerSeasonRepository).save(seasonCaptor.capture());
            PlayerSeason savedSeason = seasonCaptor.getValue();

            assertThat(savedSeason.getTotalPoints()).isEqualTo(205);
            assertThat(savedSeason.getHighestTierAchieved()).isEqualTo("Ape Operosa");
        }

        @Test
        @DisplayName("Should promote tier to 'Regina' when total points >= 500")
        void shouldPromoteToRegina() {
            season.setBasePoints(480);
            season.setBonusPoints(10);
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.of(season));

            playerSeasonService.updateSeasonProgress(userId, 20, true); // 500 base + 10 bonus = 510 total

            verify(playerSeasonRepository).save(seasonCaptor.capture());
            PlayerSeason savedSeason = seasonCaptor.getValue();

            assertThat(savedSeason.getTotalPoints()).isEqualTo(510);
            assertThat(savedSeason.getHighestTierAchieved()).isEqualTo("Regina");
        }

        @Test
        @DisplayName("Should create new season before updating progress when season is missing")
        void shouldCreateSeasonIfNotFoundDuringUpdate() {
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            when(playerSeasonRepository.save(any(PlayerSeason.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            playerSeasonService.updateSeasonProgress(userId, 60, false);

            verify(userRepository, times(1)).findById(userId);
            verify(playerSeasonRepository, times(2)).save(seasonCaptor.capture());

            PlayerSeason finalSavedSeason = seasonCaptor.getValue();
            assertThat(finalSavedSeason.getBasePoints()).isEqualTo(60);
            assertThat(finalSavedSeason.getTotalPoints()).isEqualTo(60);
            assertThat(finalSavedSeason.getHighestTierAchieved()).isEqualTo("Ape Esploratrice");
        }

        @Test
        @DisplayName("Should throw RuntimeException when user is not found during initial season creation")
        void shouldThrowExceptionWhenUserNotFound() {
            when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> playerSeasonService.updateSeasonProgress(userId, 10, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Utente non trovato con ID: " + userId);

            verify(playerSeasonRepository, never()).save(any());
        }
    }
}