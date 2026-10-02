package com.beesagono.backend.service;

import com.beesagono.backend.dto.badge.BadgeResponse;
import com.beesagono.backend.dto.badge.UserBadgesSummaryResponse;
import com.beesagono.backend.entity.Badge;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserBadge;
import com.beesagono.backend.entity.id.UserBadgeId;
import com.beesagono.backend.enums.BadgeCode;
import com.beesagono.backend.repository.BadgeRepository;
import com.beesagono.backend.repository.FoundWordRepository;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.PlayerSeasonRepository;
import com.beesagono.backend.repository.PlayerStatsRepository;
import com.beesagono.backend.repository.UserBadgeRepository;
import com.beesagono.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeServiceImplTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private PlayerStatsRepository playerStatsRepository;

    @Mock
    private PlayerSeasonRepository playerSeasonRepository;

    @Mock
    private FoundWordRepository foundWordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @InjectMocks
    private BadgeServiceImpl badgeService;

    private String userId;
    private User testUser;
    private PlayerStats playerStats;

    @BeforeEach
    void setUp() {
        userId = "user-uuid-123";

        testUser = User.builder()
                .id(userId)
                .username("TestUser")
                .email("test@example.com")
                .build();

        playerStats = PlayerStats.builder()
                .userId(userId)
                .currentStreak(5)
                .totalPuzzlesCompleted(2)
                .gamesPlayed(3)
                .build();
    }

    @Nested
    @DisplayName("getUserBadges Tests")
    class GetUserBadgesTests {

        @Test
        @DisplayName("Should get user badges successfully with correct progression and unlocked states")
        void shouldGetUserBadgesSuccessfully() {
            Instant unlockedTime = Instant.now().minusSeconds(3600);
            UserBadge userBadge = createUserBadge(userId, BadgeCode.FIRST_WORD.name(), unlockedTime);
            GameSession session = createGameSession("GENIO");

            when(userBadgeRepository.findByIdUserId(userId)).thenReturn(List.of(userBadge));
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(playerStats));
            when(gameSessionRepository.findByUserId(userId)).thenReturn(List.of(session));
            when(foundWordRepository.countBySessionUserId(userId)).thenReturn(120L);
            when(foundWordRepository.countPangramsByUserId(userId)).thenReturn(3L);
            when(playerSeasonRepository.findLatestHighestTierByUserId(userId)).thenReturn(Optional.of("WORKER"));

            List<BadgeResponse> result = badgeService.getUserBadges(userId);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(BadgeCode.values().length);

            BadgeResponse firstWordBadge = findBadgeByCode(result, BadgeCode.FIRST_WORD.name());
            assertThat(firstWordBadge).isNotNull();
            assertThat(firstWordBadge.isUnlocked()).isTrue();
            assertThat(firstWordBadge.getUnlockedAt()).isEqualTo(unlockedTime);
            assertThat(firstWordBadge.getCurrentProgress()).isEqualTo(firstWordBadge.getTargetValue());
            assertThat(firstWordBadge.getProgressPercentage()).isEqualTo(100.0);

            BadgeResponse streak100Badge = findBadgeByCode(result, BadgeCode.STREAK_100.name());
            assertThat(streak100Badge).isNotNull();
            assertThat(streak100Badge.isUnlocked()).isFalse();
            assertThat(streak100Badge.getUnlockedAt()).isNull();
            assertThat(streak100Badge.getCurrentProgress()).isEqualTo(5);
            assertThat(streak100Badge.getProgressPercentage()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("getUserBadgesSummary Tests")
    class GetUserBadgesSummaryTests {

        @Test
        @DisplayName("Should get user badges summary successfully with filters applied")
        void shouldGetUserBadgesSummaryWithFiltersSuccessfully() {
            UserBadge unlockedBadge = createUserBadge(userId, BadgeCode.FIRST_WORD.name(), Instant.now());

            when(userBadgeRepository.findByIdUserId(userId)).thenReturn(List.of(unlockedBadge));
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(playerStats));
            when(gameSessionRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(foundWordRepository.countBySessionUserId(userId)).thenReturn(1L);
            when(foundWordRepository.countPangramsByUserId(userId)).thenReturn(0L);
            when(playerSeasonRepository.findLatestHighestTierByUserId(userId)).thenReturn(Optional.empty());

            UserBadgesSummaryResponse summary = badgeService.getUserBadgesSummary(userId, "WORDS", true);

            assertThat(summary).isNotNull();
            assertThat(summary.getTotalBadges()).isEqualTo(BadgeCode.values().length);
            assertThat(summary.getUnlockedBadges()).isEqualTo(1);
            assertThat(summary.getOverallCompletionPercentage()).isGreaterThan(0.0);

            assertThat(summary.getBadges()).hasSize(1);
            assertThat(summary.getBadges().get(0).getCode()).isEqualTo(BadgeCode.FIRST_WORD.name());
            assertThat(summary.getBadges().get(0).isUnlocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("evaluateAndAwardBadges Tests")
    class EvaluateAndAwardBadgesTests {

        @Test
        @DisplayName("Should return early and not award badges when user is not found")
        void shouldNotAwardBadgesWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            badgeService.evaluateAndAwardBadges(userId);

            verify(userBadgeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should unlock streak and completion badges successfully")
        void shouldUnlockStreakAndCompletionBadgesSuccessfully() {
            playerStats.setCurrentStreak(7);
            playerStats.setTotalPuzzlesCompleted(1);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.of(playerStats));
            when(foundWordRepository.countBySessionUserId(userId)).thenReturn(0L);
            when(foundWordRepository.countPangramsByUserId(userId)).thenReturn(0L);
            when(playerSeasonRepository.findLatestHighestTierByUserId(userId)).thenReturn(Optional.empty());
            when(gameSessionRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            when(userBadgeRepository.existsByIdUserIdAndIdBadgeCode(eq(userId), anyString())).thenReturn(false);

            Badge badge = Badge.builder().code(BadgeCode.STREAK_7.name()).title("Settimana di Fuoco").build();
            when(badgeRepository.findById(anyString())).thenReturn(Optional.of(badge));

            badgeService.evaluateAndAwardBadges(userId);

            verify(userBadgeRepository, times(3)).save(any(UserBadge.class));
        }

        @Test
        @DisplayName("Should unlock historical word badges successfully preserving historical dates")
        void shouldUnlockHistoricalWordBadgesSuccessfully() {
            Date historicalDate = new Date(System.currentTimeMillis() - 86400000L);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(playerStatsRepository.findById(userId)).thenReturn(Optional.empty());
            when(foundWordRepository.countBySessionUserId(userId)).thenReturn(100L);
            when(foundWordRepository.findFirstWordDateByUserId(userId)).thenReturn(Optional.of(historicalDate));
            when(foundWordRepository.findNthWordDateByUserId(userId, 99)).thenReturn(Optional.of(historicalDate));

            when(userBadgeRepository.existsByIdUserIdAndIdBadgeCode(eq(userId), anyString())).thenReturn(false);

            Badge mockBadge = Badge.builder().code("MOCK").title("Mock").build();
            when(badgeRepository.findById(anyString())).thenReturn(Optional.of(mockBadge));

            badgeService.evaluateAndAwardBadges(userId);

            ArgumentCaptor<UserBadge> captor = ArgumentCaptor.forClass(UserBadge.class);
            verify(userBadgeRepository, atLeastOnce()).save(captor.capture());

            List<UserBadge> savedBadges = captor.getAllValues();
            boolean hasFirstWordWithHistoricalDate = savedBadges.stream()
                    .anyMatch(b -> b.getId().getBadgeCode().equals(BadgeCode.FIRST_WORD.name())
                            && b.getUnlockedAt().equals(historicalDate.toInstant()));

            assertThat(hasFirstWordWithHistoricalDate).isTrue();
        }
    }

    // --- Helper Methods ---

    private UserBadge createUserBadge(String uId, String badgeCode, Instant unlockedAt) {
        return UserBadge.builder()
                .id(new UserBadgeId(uId, badgeCode))
                .unlockedAt(unlockedAt)
                .build();
    }

    private GameSession createGameSession(String rankLabel) {
        return GameSession.builder()
                .currentRankLabel(rankLabel)
                .build();
    }

    private BadgeResponse findBadgeByCode(List<BadgeResponse> badges, String code) {
        return badges.stream()
                .filter(b -> b.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}