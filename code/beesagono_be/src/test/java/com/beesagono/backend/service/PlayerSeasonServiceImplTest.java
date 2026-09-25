package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.enums.CareerTier;
import com.beesagono.backend.repository.MilestoneRedemptionRepository;
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
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
        private MilestoneRedemptionRepository milestoneRedemptionRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private ScoringService scoringService;

        @InjectMocks
        private PlayerSeasonServiceImpl playerSeasonService;

        @Captor
        private ArgumentCaptor<PlayerSeason> seasonCaptor;

        @Captor
        private ArgumentCaptor<PlayerStats> statsCaptor;

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
                                .highestTierAchieved(CareerTier.EGG.getName())
                                .build();

                stats = PlayerStats.builder()
                                .userId(userId)
                                .currentStreak(5)
                                .lastStreakMilestoneClaimed(0)
                                .build();
        }

        @Nested
        @DisplayName("getCurrentSeasonStats Tests")
        class GetCurrentSeasonStatsTests {

                @Test
                @DisplayName("Should return existing season stats successfully")
                void shouldReturnExistingSeasonStats() {
                        when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                                        .thenReturn(Optional.of(season));

                        PlayerSeasonResponse response = playerSeasonService.getCurrentSeasonStats(userId);

                        assertThat(response).isNotNull();
                        assertThat(response.getYear()).isEqualTo(currentYear);
                        assertThat(response.getBasePoints()).isEqualTo(30);
                        assertThat(response.getBonusPoints()).isEqualTo(10);
                        assertThat(response.getTotalPoints()).isEqualTo(40);
                        assertThat(response.getHighestTierAchieved()).isEqualTo(CareerTier.EGG.getName());

                        verify(playerSeasonRepository, times(1)).findByIdUserIdAndIdSeasonYear(userId, currentYear);
                        verify(userRepository, never()).findById(any());
                }

                @Test
                @DisplayName("Should create initial season if none exists")
                void shouldCreateInitialSeasonWhenNotPresent() {
                        when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                                        .thenReturn(Optional.empty());
                        when(userRepository.findById(userId))
                                        .thenReturn(Optional.of(user));
                        when(playerSeasonRepository.save(any(PlayerSeason.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));

                        PlayerSeasonResponse response = playerSeasonService.getCurrentSeasonStats(userId);

                        assertThat(response).isNotNull();
                        assertThat(response.getYear()).isEqualTo(currentYear);
                        assertThat(response.getBasePoints()).isZero();
                        assertThat(response.getBonusPoints()).isZero();
                        assertThat(response.getTotalPoints()).isZero();
                        assertThat(response.getHighestTierAchieved()).isEqualTo(CareerTier.EGG.getName());

                        verify(userRepository, times(1)).findById(userId);
                        verify(playerSeasonRepository, times(1)).save(any(PlayerSeason.class));
                }
        }

        @Nested
        @DisplayName("updateSeasonProgress Tests")
        class UpdateSeasonProgressTests {

                @Test
                @DisplayName("Should update base points and maintain EGG tier when total points < 50")
                void shouldUpdatePointsAndKeepInitialTier() {
                        when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                                        .thenReturn(Optional.of(season));
                        when(playerStatsRepository.findById(userId))
                                        .thenReturn(Optional.of(stats));
                        when(scoringService.calculateCareerTier(anyInt(), anyInt()))
                                        .thenReturn(CareerTier.EGG);

                        playerSeasonService.updateSeasonProgress(userId, 5, false);

                        verify(playerSeasonRepository).save(seasonCaptor.capture());
                        PlayerSeason savedSeason = seasonCaptor.getValue();

                        assertThat(savedSeason.getBasePoints()).isEqualTo(35);
                        assertThat(savedSeason.getBonusPoints()).isEqualTo(10);
                        assertThat(savedSeason.getTotalPoints()).isEqualTo(45);
                        assertThat(savedSeason.getHighestTierAchieved()).isEqualTo(CareerTier.EGG.getName());
                }

                @Test
                @DisplayName("Should apply streak bonus when milestone is reached and not claimed yet")
                void shouldApplyStreakBonusWhenMilestoneReached() {
                        stats.setCurrentStreak(3);
                        stats.setLastStreakMilestoneClaimed(0);

                        when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                                        .thenReturn(Optional.of(season));
                        when(playerStatsRepository.findById(userId))
                                        .thenReturn(Optional.of(stats));
                        when(scoringService.calculateCareerTier(anyInt(), anyInt()))
                                        .thenReturn(CareerTier.EGG);

                        playerSeasonService.updateSeasonProgress(userId, 10, true);

                        verify(playerSeasonRepository).save(seasonCaptor.capture());
                        verify(playerStatsRepository).save(statsCaptor.capture());
                        verify(milestoneRedemptionRepository).save(any());

                        PlayerSeason savedSeason = seasonCaptor.getValue();
                        PlayerStats savedStats = statsCaptor.getValue();

                        assertThat(savedStats.getLastStreakMilestoneClaimed()).isEqualTo(3);
                        assertThat(savedSeason.getBasePoints()).isEqualTo(40);
                        assertThat(savedSeason.getBonusPoints()).isEqualTo(60);
                        assertThat(savedSeason.getTotalPoints()).isEqualTo(100);
                }

                @Test
                @DisplayName("Should promote tier to FORAGER_BEE when total points threshold is met")
                void shouldPromoteToApeEsploratrice() {
                        when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                                        .thenReturn(Optional.of(season));
                        when(playerStatsRepository.findById(userId))
                                        .thenReturn(Optional.of(stats));
                        when(scoringService.calculateCareerTier(anyInt(), anyInt()))
                                        .thenReturn(CareerTier.FORAGER_BEE);

                        playerSeasonService.updateSeasonProgress(userId, 15, false);

                        verify(playerSeasonRepository).save(seasonCaptor.capture());
                        PlayerSeason savedSeason = seasonCaptor.getValue();

                        assertThat(savedSeason.getTotalPoints()).isEqualTo(55);
                        assertThat(savedSeason.getHighestTierAchieved()).isEqualTo(CareerTier.FORAGER_BEE.getName());
                }

                @Test
                @DisplayName("Should promote tier to WORKER_BEE when total points threshold is met")
                void shouldPromoteToApeOperosa() {
                        season.setBasePoints(180);
                        season.setBonusPoints(10);
                        when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                                        .thenReturn(Optional.of(season));
                        when(playerStatsRepository.findById(userId))
                                        .thenReturn(Optional.of(stats));
                        when(scoringService.calculateCareerTier(anyInt(), anyInt()))
                                        .thenReturn(CareerTier.WORKER_BEE);

                        playerSeasonService.updateSeasonProgress(userId, 15, false);

                        verify(playerSeasonRepository).save(seasonCaptor.capture());
                        PlayerSeason savedSeason = seasonCaptor.getValue();

                        assertThat(savedSeason.getTotalPoints()).isEqualTo(205);
                        assertThat(savedSeason.getHighestTierAchieved()).isEqualTo(CareerTier.WORKER_BEE.getName());
                }

                @Test
                @DisplayName("Should promote tier to SEASON_QUEEN when total points threshold is met")
                void shouldPromoteToRegina() {
                        season.setBasePoints(480);
                        season.setBonusPoints(10);
                        when(playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear))
                                        .thenReturn(Optional.of(season));
                        when(playerStatsRepository.findById(userId))
                                        .thenReturn(Optional.of(stats));
                        when(scoringService.calculateCareerTier(anyInt(), anyInt()))
                                        .thenReturn(CareerTier.SEASON_QUEEN);

                        playerSeasonService.updateSeasonProgress(userId, 20, true);

                        verify(playerSeasonRepository).save(seasonCaptor.capture());
                        PlayerSeason savedSeason = seasonCaptor.getValue();

                        assertThat(savedSeason.getTotalPoints()).isEqualTo(510);
                        assertThat(savedSeason.getHighestTierAchieved()).isEqualTo(CareerTier.SEASON_QUEEN.getName());
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

        @Nested
        @DisplayName("getPlayerSeasonHistory Tests")
        class GetPlayerSeasonHistoryTests {

                @Test
                @DisplayName("Should return list of historical seasons for user")
                void shouldReturnSeasonHistory() {
                        PlayerSeason season2025 = PlayerSeason.builder()
                                        .id(new PlayerSeasonId(userId, 2025))
                                        .user(user)
                                        .basePoints(100)
                                        .bonusPoints(20)
                                        .totalPoints(120)
                                        .highestTierAchieved(CareerTier.FORAGER_BEE.getName())
                                        .build();

                        when(playerSeasonRepository.findByUserId(userId))
                                        .thenReturn(List.of(season, season2025));

                        List<PlayerSeasonResponse> history = playerSeasonService.getPlayerSeasonHistory(userId);

                        assertThat(history).hasSize(2);
                        assertThat(history.get(0).getYear()).isEqualTo(currentYear);
                        assertThat(history.get(1).getYear()).isEqualTo(2025);

                        verify(playerSeasonRepository, times(1)).findByUserId(userId);
                }
        }

        @Nested
        @DisplayName("getRankDistribution Tests")
        class GetRankDistributionTests {

                @Test
                @DisplayName("Should map tier count aggregate query results into RankDistributionResponse")
                void shouldReturnRankDistribution() {
                        List<Object[]> rawResults = List.<Object[]>of(
                                        new Object[] { CareerTier.EGG.getName(), 10L },
                                        new Object[] { CareerTier.WORKER_BEE.getName(), 5L });

                        when(playerSeasonRepository.countPlayersByTierForYear(currentYear))
                                        .thenReturn(rawResults);

                        RankDistributionResponse response = playerSeasonService.getRankDistribution();

                        assertThat(response).isNotNull();
                        assertThat(response.getSeasonYear()).isEqualTo(currentYear);
                        assertThat(response.getTotalPlayers()).isEqualTo(15L);
                        assertThat(response.getTierCounts()).containsEntry(CareerTier.EGG.getName(), 10L);
                        assertThat(response.getTierCounts()).containsEntry(CareerTier.WORKER_BEE.getName(), 5L);

                        verify(playerSeasonRepository, times(1)).countPlayersByTierForYear(currentYear);
                }
        }

        @Nested
        @DisplayName("getTopLeaderboard Tests")
        class GetTopLeaderboardTests {

                @Test
                @DisplayName("Should fetch top players and assign sequential rank positions")
                void shouldReturnTopLeaderboardWithPositions() {
                        when(playerSeasonRepository.findTopPlayersByYear(currentYear, PageRequest.of(0, 10)))
                                        .thenReturn(List.of(season));

                        List<LeaderboardEntryDto> leaderboard = playerSeasonService.getTopLeaderboard(10);

                        assertThat(leaderboard).hasSize(1);
                        assertThat(leaderboard.get(0).getUsername()).isEqualTo("testplayer");
                        assertThat(leaderboard.get(0).getTotalPoints()).isEqualTo(40);
                        assertThat(leaderboard.get(0).getHighestTierAchieved()).isEqualTo(CareerTier.EGG.getName());
                        assertThat(leaderboard.get(0).getRankPosition()).isEqualTo(1);

                        verify(playerSeasonRepository, times(1)).findTopPlayersByYear(currentYear,
                                        PageRequest.of(0, 10));
                }
        }
}