package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.LeaderboardEntryDto;
import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.dto.stats.RankDistributionResponse;
import com.beesagono.backend.entity.MilestoneRedemption;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.MilestoneRedemptionId;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.enums.CareerTier;
import com.beesagono.backend.enums.GameConstants;
import com.beesagono.backend.repository.MilestoneRedemptionRepository;
import com.beesagono.backend.repository.PlayerSeasonRepository;
import com.beesagono.backend.repository.PlayerStatsRepository;
import com.beesagono.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerSeasonServiceImpl implements PlayerSeasonService {

    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final MilestoneRedemptionRepository milestoneRedemptionRepository;
    private final UserRepository userRepository;
    private final ScoringService scoringService;

    // Target annuale di punti stimato per la percentuale di CareerTier
    private static final int ANNUAL_TARGET_POINTS = 5000;

    @Override
    @Transactional(readOnly = true)
    public PlayerSeasonResponse getCurrentSeasonStats(String userId) {
        int currentYear = LocalDate.now().getYear();

        PlayerSeason season = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear)
                .orElseGet(() -> createInitialSeason(userId, currentYear));

        return mapToResponse(season);
    }

    @Override
    @Transactional
    public void updateSeasonProgress(String userId, int pointsEarned, boolean isGameCompleted) {
        int currentYear = LocalDate.now().getYear();

        PlayerSeason season = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear)
                .orElseGet(() -> createInitialSeason(userId, currentYear));

        PlayerStats stats = playerStatsRepository.findById(userId)
                .orElseGet(() -> PlayerStats.builder().userId(userId).build());

        // Update base points
        season.setBasePoints(season.getBasePoints() + pointsEarned);

        // Check and apply the Streak bonus
        int currentStreak = stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0;
        int streakBonus = GameConstants.STREAK_MILESTONES.getOrDefault(currentStreak, 0);

        if (streakBonus > 0 && shouldApplyStreakBonus(stats, currentStreak)) {
            season.setBonusPoints(season.getBonusPoints() + streakBonus);

            MilestoneRedemption redemption = MilestoneRedemption.builder()
                    .id(new MilestoneRedemptionId(userId, currentYear, currentStreak))
                    .user(season.getUser())
                    .playerSeason(season)
                    .redeemedAt(new Date())
                    .build();
            milestoneRedemptionRepository.save(redemption);

            // Update the status of redeemed milestones in the user statistics
            stats.setLastStreakMilestoneClaimed(currentStreak);
            playerStatsRepository.save(stats);
        }

        // Recalculate total and Tier
        season.setTotalPoints(season.getBasePoints() + season.getBonusPoints());

        CareerTier tier = scoringService.calculateCareerTier(season.getTotalPoints(), ANNUAL_TARGET_POINTS);
        season.setHighestTierAchieved(tier.getName());

        playerSeasonRepository.save(season);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerSeasonResponse> getPlayerSeasonHistory(String userId) {
        return playerSeasonRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RankDistributionResponse getRankDistribution() {
        int currentYear = LocalDate.now().getYear();

        List<Object[]> results = playerSeasonRepository.countPlayersByTierForYear(currentYear);
        Map<String, Long> tierCounts = new HashMap<>();
        long totalPlayers = 0;

        for (Object[] result : results) {
            String tier = (String) result[0];
            Long count = (Long) result[1];
            tierCounts.put(tier, count);
            totalPlayers += count;
        }

        return RankDistributionResponse.builder()
                .seasonYear(currentYear)
                .totalPlayers(totalPlayers)
                .tierCounts(tierCounts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getTopLeaderboard(int limit) {
        int currentYear = LocalDate.now().getYear();
        List<PlayerSeason> topSeasons = playerSeasonRepository.findTopPlayersByYear(
                currentYear, PageRequest.of(0, limit));

        List<LeaderboardEntryDto> leaderboard = new ArrayList<>();
        int position = 1;

        for (PlayerSeason season : topSeasons) {
            leaderboard.add(LeaderboardEntryDto.builder()
                    .username(season.getUser() != null ? season.getUser().getUsername() : "Anonimo")
                    .totalPoints(season.getTotalPoints())
                    .highestTierAchieved(season.getHighestTierAchieved())
                    .rankPosition(position++)
                    .build());
        }

        return leaderboard;
    }

    // --- Private Helper Methods ---

    private boolean shouldApplyStreakBonus(PlayerStats stats, int currentStreak) {
        Integer lastClaimed = stats.getLastStreakMilestoneClaimed();
        return lastClaimed == null || lastClaimed < currentStreak;
    }

    private PlayerSeason createInitialSeason(String userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));

        PlayerSeasonId id = new PlayerSeasonId(userId, year);

        PlayerSeason season = PlayerSeason.builder()
                .id(id)
                .user(user)
                .basePoints(0)
                .bonusPoints(0)
                .totalPoints(0)
                .highestTierAchieved(CareerTier.EGG.getName())
                .build();

        return playerSeasonRepository.save(season);
    }

    private PlayerSeasonResponse mapToResponse(PlayerSeason season) {
        return PlayerSeasonResponse.builder()
                .year(season.getId() != null ? season.getId().getSeasonYear() : null)
                .highestTierAchieved(season.getHighestTierAchieved())
                .basePoints(season.getBasePoints())
                .bonusPoints(season.getBonusPoints())
                .totalPoints(season.getTotalPoints())
                .build();
    }
}