package com.beesagono.backend.service;

import com.beesagono.backend.dto.badge.BadgeResponse;
import com.beesagono.backend.dto.badge.UserBadgesSummaryResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final GameSessionRepository gameSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponse> getUserBadges(String userId) {
        // Uses the correct repository method name
        List<UserBadge> unlockedBadges = userBadgeRepository.findByIdUserId(userId);

        Map<String, Instant> unlockedMap = unlockedBadges.stream()
                .collect(Collectors.toMap(
                        ub -> ub.getId().getBadgeCode(),
                        UserBadge::getUnlockedAt));

        PlayerStats stats = playerStatsRepository.findById(userId).orElse(null);
        int currentStreak = (stats != null && stats.getCurrentStreak() != null) ? stats.getCurrentStreak() : 0;

        int completedPuzzles = (stats != null && stats.getTotalPuzzlesCompleted() != null)
                ? stats.getTotalPuzzlesCompleted()
                : ((stats != null && stats.getGamesPlayed() != null) ? stats.getGamesPlayed() : 0);

        // RANK: Finds the maximum rank level ever achieved by the user across all game
        // sessions
        List<GameSession> userSessions = gameSessionRepository.findByUserId(userId);
        int rankIndex = userSessions.stream()
                .mapToInt(session -> getRankIndex(session.getCurrentRankLabel()))
                .max()
                .orElse(0);

        int totalWords = (int) foundWordRepository.countBySessionUserId(userId);
        int totalPangrams = (int) foundWordRepository.countPangramsByUserId(userId);

        // CAREER: Finds the most recent career tier in the DB
        String latestCareerTier = playerSeasonRepository.findLatestHighestTierByUserId(userId).orElse("NONE");
        int careerTierIndex = getCareerTierIndex(latestCareerTier);

        return Arrays.stream(BadgeCode.values())
                .map(badge -> {
                    boolean isUnlocked = unlockedMap.containsKey(badge.name());
                    int target = badge.getTargetValue();
                    int current = calculateCurrentProgress(badge, currentStreak, completedPuzzles, totalWords,
                            totalPangrams, careerTierIndex, rankIndex);

                    if (isUnlocked) {
                        current = target;
                    }

                    double percentage = target > 0 ? Math.min(100.0, ((double) current / target) * 100.0) : 0.0;

                    return BadgeResponse.builder()
                            .code(badge.name())
                            .title(badge.getTitle())
                            .description(badge.getDescription())
                            .category(badge.getCategory())
                            .iconUrl(badge.getIconUrl())
                            .unlocked(isUnlocked)
                            .unlockedAt(isUnlocked ? unlockedMap.get(badge.name()) : null)
                            .currentProgress(current)
                            .targetValue(target)
                            .progressPercentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserBadgesSummaryResponse getUserBadgesSummary(String userId, String category, Boolean unlockedOnly) {
        List<BadgeResponse> allBadges = getUserBadges(userId);

        int totalBadges = allBadges.size();
        int unlockedCount = (int) allBadges.stream().filter(BadgeResponse::isUnlocked).count();
        double overallPercentage = totalBadges > 0
                ? Math.round(((double) unlockedCount / totalBadges) * 100.0 * 100.0) / 100.0
                : 0.0;

        List<BadgeResponse> filteredBadges = allBadges.stream()
                .filter(b -> category == null || category.trim().isEmpty()
                        || category.equalsIgnoreCase(b.getCategory()))
                .filter(b -> unlockedOnly == null || !unlockedOnly || b.isUnlocked())
                .collect(Collectors.toList());

        return UserBadgesSummaryResponse.builder()
                .totalBadges(totalBadges)
                .unlockedBadges(unlockedCount)
                .overallCompletionPercentage(overallPercentage)
                .badges(filteredBadges)
                .build();
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
        playerSeasonRepository.findLatestHighestTierByUserId(userId).ifPresent(highestTier -> {
            evaluateCareerBadges(user, highestTier);
        });

        // Rank Progress (Maximum rank achieved across all sessions)
        List<GameSession> sessions = gameSessionRepository.findByUserId(userId);
        int maxRankIndex = sessions.stream()
                .mapToInt(session -> getRankIndex(session.getCurrentRankLabel()))
                .max()
                .orElse(0);

        if (maxRankIndex > 0) {
            evaluateRankBadgesWithIndex(user, maxRankIndex);
        }
    }

    // --- Private Helper Methods ---

    private int calculateCurrentProgress(BadgeCode badge, int streak, int completed, int words, int pangrams,
            int careerIndex, int rankIndex) {
        return switch (badge.getCategory()) {
            case "STREAK" -> streak;
            case "COMPLETION" -> completed;
            case "WORDS" -> words;
            case "PANGRAM" -> pangrams;
            case "CAREER" -> careerIndex;
            case "RANK" -> rankIndex;
            default -> 0;
        };
    }

    private int getRankIndex(String rankLabel) {
        if (rankLabel == null)
            return 0;

        String normalized = rankLabel.toUpperCase();

        if (normalized.contains("REGINA") || normalized.contains("QUEEN") || normalized.contains("MASTER_QUEEN"))
            return 8;
        if (normalized.contains("MAESTRO") || normalized.contains("MASTER"))
            return 7;
        if (normalized.contains("GENIO") || normalized.contains("GENIUS"))
            return 6;
        if (normalized.contains("ECCELLENTE") || normalized.contains("EXCELLENT"))
            return 5;
        if (normalized.contains("ESPERTO") || normalized.contains("EXPERT"))
            return 4;
        if (normalized.contains("AVANZATO") || normalized.contains("ADVANCED"))
            return 3;
        if (normalized.contains("PRINCIPIANTE") || normalized.contains("BEGINNER"))
            return 2;
        if (normalized.contains("MENTE FRESCA") || normalized.contains("FRESH_MIND"))
            return 1;

        return 0;
    }

    private int getCareerTierIndex(String tierStr) {
        if (tierStr == null || "NONE".equalsIgnoreCase(tierStr)) {
            return 0;
        }

        String normalized = tierStr.toUpperCase();

        if (normalized.contains("SEASON_QUEEN") || normalized.contains("REGINA") || normalized.contains("QUEEN"))
            return 8;
        if (normalized.contains("ARCHITECT") || normalized.contains("ARCHITETTO"))
            return 7;
        if (normalized.contains("DEFENDER") || normalized.contains("DIFENSORE"))
            return 6;
        if (normalized.contains("GUARDIAN") || normalized.contains("GUARDIANA"))
            return 5;
        if (normalized.contains("FORAGER") || normalized.contains("BOTTINATRICE"))
            return 4;
        if (normalized.contains("WORKER") || normalized.contains("OPERAIA"))
            return 3;
        if (normalized.contains("NURSE") || normalized.contains("NUTRICE"))
            return 2;
        if (normalized.contains("LARVA"))
            return 1;

        // "UOVO" / "Bee Egg" is the initial stage before Larva -> Index 0
        if (normalized.contains("UOVO") || normalized.contains("EGG"))
            return 0;

        return 0;
    }

    private void evaluateStreakBadges(User user, PlayerStats stats) {
        int streak = stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0;
        for (BadgeCode badge : BadgeCode.values()) {
            if ("STREAK".equals(badge.getCategory()) && streak >= badge.getTargetValue()) {
                unlockBadge(user, badge);
            }
        }
    }

    private void evaluateCompletionBadges(User user, PlayerStats stats) {
        int completed = stats.getTotalPuzzlesCompleted() != null ? stats.getTotalPuzzlesCompleted() : 0;
        for (BadgeCode badge : BadgeCode.values()) {
            if ("COMPLETION".equals(badge.getCategory()) && completed >= badge.getTargetValue()) {
                unlockBadge(user, badge);
            }
        }
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

    private void evaluateCareerBadges(User user, String highestTier) {
        int userTierIndex = getCareerTierIndex(highestTier);
        if (userTierIndex == 0)
            return;

        for (BadgeCode badge : BadgeCode.values()) {
            if ("CAREER".equalsIgnoreCase(badge.getCategory()) && userTierIndex >= badge.getTargetValue()) {
                unlockBadge(user, badge);
            }
        }
    }

    private void evaluateRankBadgesWithIndex(User user, int maxRankIndex) {
        for (BadgeCode badge : BadgeCode.values()) {
            if ("RANK".equalsIgnoreCase(badge.getCategory()) && maxRankIndex >= badge.getTargetValue()) {
                unlockBadge(user, badge);
            }
        }
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