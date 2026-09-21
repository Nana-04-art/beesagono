package com.beesagono.backend.service;

import com.beesagono.backend.dto.badge.BadgeResponse;
import com.beesagono.backend.enums.BadgeCode;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.UserBadge;
import com.beesagono.backend.entity.id.UserBadgeId;
import com.beesagono.backend.repository.BadgeRepository;
import com.beesagono.backend.repository.FoundWordRepository;
import com.beesagono.backend.repository.PlayerSeasonRepository;
import com.beesagono.backend.repository.PlayerStatsRepository;
import com.beesagono.backend.repository.UserBadgeRepository;
import com.beesagono.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final FoundWordRepository foundWordRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getUserBadges(String userId) {
        // Uses the correct repository method name
        List<UserBadge> unlockedBadges = userBadgeRepository.findByIdUserId(userId);

        Map<String, Instant> unlockedMap = unlockedBadges.stream()
                .collect(Collectors.toMap(
                        ub -> ub.getId().getBadgeCode(),
                        UserBadge::getUnlockedAt));

        return Arrays.stream(BadgeCode.values())
                .map(badge -> {
                    boolean isUnlocked = unlockedMap.containsKey(badge.name());
                    return BadgeResponse.builder()
                            .code(badge.name())
                            .title(badge.getTitle())
                            .description(badge.getDescription())
                            .category(badge.getCategory())
                            .iconUrl(badge.getIconUrl())
                            .unlocked(isUnlocked)
                            .unlockedAt(isUnlocked ? unlockedMap.get(badge.name()) : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void evaluateAndAwardBadges(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return;

        // Streak & Completions
        playerStatsRepository.findById(userId).ifPresent(stats -> {
            evaluateStreakBadges(user, stats);
            evaluateCompletionBadges(user, stats);
        });

        // Found Words with accurate historical timestamp
        long totalWords = foundWordRepository.countBySessionUserId(userId);
        evaluateWordBadgesHistorical(user, (int) totalWords);

        // Pangrams with accurate historical timestamp
        long totalPangrams = foundWordRepository.countPangramsByUserId(userId);
        evaluatePangramBadgesHistorical(user, (int) totalPangrams);

        // Seasonal Career Progress
        int currentYear = LocalDate.now().getYear();
        playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear).ifPresent(season -> {
            evaluateCareerBadges(user, season);
        });
    }

    private void evaluateStreakBadges(User user, PlayerStats stats) {
        int streak = stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0;
        if (streak >= 3)
            unlockBadge(user, BadgeCode.STREAK_3);
        if (streak >= 7)
            unlockBadge(user, BadgeCode.STREAK_7);
        if (streak >= 15)
            unlockBadge(user, BadgeCode.STREAK_15);
        if (streak >= 30)
            unlockBadge(user, BadgeCode.STREAK_30);
        if (streak >= 50)
            unlockBadge(user, BadgeCode.STREAK_50);
        if (streak >= 100)
            unlockBadge(user, BadgeCode.STREAK_100);
        if (streak >= 200)
            unlockBadge(user, BadgeCode.STREAK_200);
        if (streak >= 365)
            unlockBadge(user, BadgeCode.STREAK_365);
    }

    private void evaluateCompletionBadges(User user, PlayerStats stats) {
        int completed = stats.getTotalPuzzlesCompleted() != null ? stats.getTotalPuzzlesCompleted() : 0;
        if (completed >= 1)
            unlockBadge(user, BadgeCode.FIRST_COMPLETION);
        if (completed >= 10)
            unlockBadge(user, BadgeCode.COMPLETION_10);
    }

    private void evaluateWordBadgesHistorical(User user, int totalWords) {
        if (totalWords >= 1) {
            Instant instant = foundWordRepository.findFirstWordDateByUserId(user.getId())
                    .map(Date::toInstant).orElseGet(Instant::now);
            unlockBadgeWithDate(user, BadgeCode.FIRST_WORD, instant);
        }
        if (totalWords >= 100) {
            Instant instant = foundWordRepository.findNthWordDateByUserId(user.getId(), 99)
                    .map(Date::toInstant).orElseGet(Instant::now);
            unlockBadgeWithDate(user, BadgeCode.WORDS_100, instant);
        }
        if (totalWords >= 500) {
            Instant instant = foundWordRepository.findNthWordDateByUserId(user.getId(), 499)
                    .map(Date::toInstant).orElseGet(Instant::now);
            unlockBadgeWithDate(user, BadgeCode.WORDS_500, instant);
        }
        if (totalWords >= 1000) {
            Instant instant = foundWordRepository.findNthWordDateByUserId(user.getId(), 999)
                    .map(Date::toInstant).orElseGet(Instant::now);
            unlockBadgeWithDate(user, BadgeCode.WORDS_1000, instant);
        }
    }

    private void evaluatePangramBadgesHistorical(User user, int totalPangrams) {
        if (totalPangrams >= 1) {
            Instant instant = foundWordRepository.findFirstPangramDateByUserId(user.getId())
                    .map(Date::toInstant).orElseGet(Instant::now);
            unlockBadgeWithDate(user, BadgeCode.FIRST_PANGRAM, instant);
        }
        if (totalPangrams >= 10) {
            Instant instant = foundWordRepository.findNthPangramDateByUserId(user.getId(), 9)
                    .map(Date::toInstant).orElseGet(Instant::now);
            unlockBadgeWithDate(user, BadgeCode.PANGRAM_10, instant);
        }
        if (totalPangrams >= 50) {
            Instant instant = foundWordRepository.findNthPangramDateByUserId(user.getId(), 49)
                    .map(Date::toInstant).orElseGet(Instant::now);
            unlockBadgeWithDate(user, BadgeCode.PANGRAM_50, instant);
        }
    }

    private void evaluateCareerBadges(User user, PlayerSeason season) {
        String highestTier = season.getHighestTierAchieved();
        if (highestTier == null)
            return;

        List<String> tiersOrder = List.of(
                "LARVA", "NURSE_BEE", "WORKER_BEE", "FORAGER_BEE",
                "GUARDIAN_BEE", "DEFENDER_BEE", "ARCHITECT_BEE", "SEASON_QUEEN");

        int userTierIndex = tiersOrder.indexOf(highestTier);
        if (userTierIndex == -1)
            return;

        if (userTierIndex >= 0)
            unlockBadge(user, BadgeCode.CAREER_LARVA);
        if (userTierIndex >= 1)
            unlockBadge(user, BadgeCode.CAREER_NURSE_BEE);
        if (userTierIndex >= 2)
            unlockBadge(user, BadgeCode.CAREER_WORKER_BEE);
        if (userTierIndex >= 3)
            unlockBadge(user, BadgeCode.CAREER_FORAGER_BEE);
        if (userTierIndex >= 4)
            unlockBadge(user, BadgeCode.CAREER_GUARDIAN_BEE);
        if (userTierIndex >= 5)
            unlockBadge(user, BadgeCode.CAREER_DEFENDER_BEE);
        if (userTierIndex >= 6)
            unlockBadge(user, BadgeCode.CAREER_ARCHITECT_BEE);
        if (userTierIndex >= 7)
            unlockBadge(user, BadgeCode.CAREER_SEASON_QUEEN);
    }

    private void unlockBadgeWithDate(User user, BadgeCode badgeCode, Instant unlockedAt) {
        if (!userBadgeRepository.existsByIdUserIdAndIdBadgeCode(user.getId(), badgeCode.name())) {
            badgeRepository.findById(badgeCode.name()).ifPresent(badge -> {
                UserBadge userBadge = UserBadge.builder()
                        .id(new UserBadgeId(user.getId(), badgeCode.name()))
                        .user(user)
                        .badge(badge)
                        .unlockedAt(unlockedAt != null ? unlockedAt : Instant.now())
                        .build();
                userBadgeRepository.save(userBadge);
            });
        }
    }

    private void unlockBadge(User user, BadgeCode badgeCode) {
        unlockBadgeWithDate(user, badgeCode, Instant.now());
    }
}