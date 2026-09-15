package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerSeasonResponse;
import com.beesagono.backend.entity.PlayerSeason;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.PlayerSeasonId;
import com.beesagono.backend.repository.PlayerSeasonRepository;
import com.beesagono.backend.repository.PlayerStatsRepository;
import com.beesagono.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PlayerSeasonServiceImpl implements PlayerSeasonService {

    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PlayerSeasonResponse getCurrentSeasonStats(String userId) {
        int currentYear = LocalDate.now().getYear();

        PlayerSeason season = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear)
                .orElseGet(() -> createInitialSeason(userId, currentYear));

        PlayerStats stats = playerStatsRepository.findById(userId)
                .orElseGet(() -> PlayerStats.builder().build());

        return mapToResponse(season, stats);
    }

    @Override
    @Transactional
    public void updateSeasonProgress(String userId, int pointsEarned, boolean isGameCompleted) {
        int currentYear = LocalDate.now().getYear();

        PlayerSeason season = playerSeasonRepository.findByIdUserIdAndIdSeasonYear(userId, currentYear)
                .orElseGet(() -> createInitialSeason(userId, currentYear));

        // Update base points and total seasonal points
        season.setBasePoints(season.getBasePoints() + pointsEarned);
        season.setTotalPoints(season.getBasePoints() + season.getBonusPoints());

        // Calculate and update the achieved rank tier
        season.setHighestTierAchieved(calculateTier(season.getTotalPoints()));

        playerSeasonRepository.save(season);
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
                .highestTierAchieved("Uovo d'Ape")
                .build();

        return playerSeasonRepository.save(season);
    }

    private String calculateTier(int totalPoints) {
        if (totalPoints >= 500)
            return "Regina";
        if (totalPoints >= 200)
            return "Ape Operosa";
        if (totalPoints >= 50)
            return "Ape Esploratrice";
        return "Uovo d'Ape";
    }

    private PlayerSeasonResponse mapToResponse(PlayerSeason season, PlayerStats stats) {
        return PlayerSeasonResponse.builder()
                .year(season.getId() != null ? season.getId().getSeasonYear() : null)
                .highestTierAchieved(season.getHighestTierAchieved())
                .basePoints(season.getBasePoints())
                .bonusPoints(season.getBonusPoints())
                .totalPoints(season.getTotalPoints())
                .gamesPlayed(stats.getGamesPlayed() != null ? stats.getGamesPlayed() : 0)
                .gamesCompleted(stats.getGamesCompleted() != null ? stats.getGamesCompleted() : 0)
                .currentStreak(stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0)
                .maxStreak(stats.getMaxStreak() != null ? stats.getMaxStreak() : 0)
                .build();
    }
}