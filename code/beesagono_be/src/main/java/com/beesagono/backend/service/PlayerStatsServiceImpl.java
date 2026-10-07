package com.beesagono.backend.service;

import com.beesagono.backend.dto.stats.PlayerStatsResponse;
import com.beesagono.backend.dto.stats.StatsSyncRequest;
import com.beesagono.backend.entity.PlayerStats;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.PlayerStatsRepository;
import com.beesagono.backend.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerStatsServiceImpl implements PlayerStatsService {

    private final PlayerStatsRepository playerStatsRepository;
    private final GameSessionRepository gameSessionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PlayerStatsResponse getPlayerStats(String userId) {
        // Retrieve player stats or create initial record if not found
        PlayerStats stats = playerStatsRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Utente non trovato con ID: " + userId));
                    return createInitialPlayerStats(user);
                });

        // Recalculate streak based on recorded daily game activity
        refreshStreak(stats, userId);

        // Save updated statistics
        PlayerStats updatedStats = playerStatsRepository.save(stats);

        return buildPlayerStatsResponse(updatedStats);
    }

    @Override
    @Transactional
    public void updatePlayerStatsAfterGame(String userId, int newPoints, String longestWord, boolean isCompleted,
            boolean hasPlayedWords) {
        PlayerStats stats = getOrCreatePlayerStats(userId);

        // Update lastPlayedDate and increment gamesPlayed only if words were submitted
        // or points earned
        if (hasPlayedWords || newPoints > 0) {
            LocalDate today = LocalDate.now();
            if (stats.getLastPlayedDate() == null || !stats.getLastPlayedDate().equals(today)) {
                stats.setGamesPlayed((stats.getGamesPlayed() != null ? stats.getGamesPlayed() : 0) + 1);
                stats.setLastPlayedDate(today);
            }
        }

        if (newPoints > 0) {
            stats.setTotalPoints((stats.getTotalPoints() != null ? stats.getTotalPoints() : 0) + newPoints);
            stats.setTotalScoreEarned(
                    (stats.getTotalScoreEarned() != null ? stats.getTotalScoreEarned() : 0) + newPoints);
        }

        if (isCompleted) {
            stats.setGamesCompleted((stats.getGamesCompleted() != null ? stats.getGamesCompleted() : 0) + 1);
            stats.setTotalPuzzlesCompleted(
                    (stats.getTotalPuzzlesCompleted() != null ? stats.getTotalPuzzlesCompleted() : 0) + 1);
        }

        if (longestWord != null && !longestWord.isBlank()) {
            if (stats.getLongestWordFound() == null || longestWord.length() > stats.getLongestWordFound().length()) {
                stats.setLongestWordFound(longestWord);
            }
        }

        // Flush changes to ensure database persistence before calculating streaks
        stats = playerStatsRepository.saveAndFlush(stats);

        refreshStreak(stats, userId);
        playerStatsRepository.save(stats);
    }

    @Override
    @Transactional
    public PlayerStatsResponse syncLocalStatsWithServer(String userId, StatsSyncRequest request) {
        if (request == null) {
            return getPlayerStats(userId);
        }

        PlayerStats stats = getOrCreatePlayerStats(userId);

        // Conservative merge strategy: retain highest values between client and server
        if (request.getGamesPlayed() != null) {
            stats.setGamesPlayed(
                    Math.max(stats.getGamesPlayed() != null ? stats.getGamesPlayed() : 0, request.getGamesPlayed()));
        }

        if (request.getGamesCompleted() != null) {
            stats.setGamesCompleted(Math.max(stats.getGamesCompleted() != null ? stats.getGamesCompleted() : 0,
                    request.getGamesCompleted()));
        }

        if (request.getMaxStreak() != null) {
            stats.setMaxStreak(
                    Math.max(stats.getMaxStreak() != null ? stats.getMaxStreak() : 0, request.getMaxStreak()));
        }

        if (request.getCurrentStreak() != null) {
            stats.setCurrentStreak(Math.max(stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0,
                    request.getCurrentStreak()));
        }

        // Sync streak milestones claimed from the client
        if (request.getClaimedStreakMilestones() != null && !request.getClaimedStreakMilestones().isEmpty()) {
            int maxClaimedInClient = request.getClaimedStreakMilestones().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);

            int currentDbClaimed = stats.getLastStreakMilestoneClaimed() != null ? stats.getLastStreakMilestoneClaimed()
                    : 0;
            stats.setLastStreakMilestoneClaimed(Math.max(currentDbClaimed, maxClaimedInClient));
        }

        if (request.getLastPlayedDate() != null && !request.getLastPlayedDate().isBlank()) {
            try {
                LocalDate clientLastPlayed = LocalDate.parse(request.getLastPlayedDate());
                if (stats.getLastPlayedDate() == null || clientLastPlayed.isAfter(stats.getLastPlayedDate())) {
                    stats.setLastPlayedDate(clientLastPlayed);
                }
            } catch (Exception ignored) {
            }
        }

        refreshStreak(stats, userId);
        PlayerStats savedStats = playerStatsRepository.save(stats);

        return buildPlayerStatsResponse(savedStats);
    }

    // --- Helper Methods ---

    private PlayerStats createInitialPlayerStats(User user) {
        PlayerStats newStats = PlayerStats.builder()
                .userId(user.getId())
                .user(user)
                .totalPoints(0)
                .gamesPlayed(0)
                .gamesCompleted(0)
                .currentStreak(0)
                .maxStreak(0)
                .longestWordFound("")
                .totalScoreEarned(0)
                .totalPuzzlesCompleted(0)
                .build();

        return playerStatsRepository.save(newStats);
    }

    private PlayerStats getOrCreatePlayerStats(String userId) {
        return playerStatsRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseGet(() -> userRepository.findByUsername(userId)
                                    .orElseThrow(() -> new EntityNotFoundException("Utente non trovato con ID o Username: " + userId)));

                    return createInitialPlayerStats(user);
                });
    }

    private void refreshStreak(PlayerStats stats, String userId) {
        int calculatedStreak = calculateCurrentStreak(userId);
        stats.setCurrentStreak(calculatedStreak);

        int maxStreak = stats.getMaxStreak() != null ? stats.getMaxStreak() : 0;
        if (calculatedStreak > maxStreak) {
            stats.setMaxStreak(calculatedStreak);
        }
    }

    /**
     * Calculates the streak of consecutive days based on the dates of played
     * puzzles.
     */
    private int calculateCurrentStreak(String userId) {
        List<LocalDate> playedDates = gameSessionRepository.findDistinctPlayedPuzzleDatesByUserId(userId);

        if (playedDates == null || playedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate mostRecentDate = playedDates.get(0);

        // Reset streak to 0 if the user has not played today or yesterday
        if (!mostRecentDate.equals(today) && !mostRecentDate.equals(yesterday)) {
            return 0;
        }

        int streak = 1;
        LocalDate previousDate = mostRecentDate;

        for (int i = 1; i < playedDates.size(); i++) {
            LocalDate currentDate = playedDates.get(i);
            if (currentDate.equals(previousDate.minusDays(1))) {
                streak++;
                previousDate = currentDate;
            } else if (currentDate.equals(previousDate)) {
                // Skip duplicate dates
                continue;
            } else {
                // Break in consecutive day count detected
                break;
            }
        }
        return streak;
    }

    private PlayerStatsResponse buildPlayerStatsResponse(PlayerStats stats) {
        String userId = stats.getUserId();
        if (userId == null && stats.getUser() != null) {
            userId = stats.getUser().getId();
        }

        int gamesPlayed = stats.getGamesPlayed() != null ? stats.getGamesPlayed() : 0;
        int gamesCompleted = stats.getGamesCompleted() != null ? stats.getGamesCompleted() : 0;
        int totalScore = stats.getTotalScoreEarned() != null ? stats.getTotalScoreEarned() : 0;

        double averageScore = gamesPlayed > 0 ? (double) totalScore / gamesPlayed : 0.0;
        double completionRate = gamesPlayed > 0 ? ((double) gamesCompleted / gamesPlayed) * 100.0 : 0.0;

        return PlayerStatsResponse.builder()
                .userId(userId)
                .gamesPlayed(gamesPlayed)
                .gamesCompleted(gamesCompleted)
                .currentStreak(stats.getCurrentStreak() != null ? stats.getCurrentStreak() : 0)
                .maxStreak(stats.getMaxStreak() != null ? stats.getMaxStreak() : 0)
                .longestWordFound(stats.getLongestWordFound() != null ? stats.getLongestWordFound() : "")
                .totalScoreEarned(totalScore)
                .averageScorePerGame(averageScore)
                .completionRate(completionRate)
                .build();
    }
}