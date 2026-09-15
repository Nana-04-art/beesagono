package com.beesagono.backend.service;

import com.beesagono.backend.enums.CareerTier;
import com.beesagono.backend.enums.RankTier;
import org.springframework.stereotype.Service;

@Service
public class ScoringServiceImpl implements ScoringService {

    private static final int MIN_WORD_LENGTH = 4;
    private static final int MIELEGRAMMA_BONUS = 7;
    private static final int REQUIRED_UNIQUE_LETTERS = 7;

    // Calculates the score for a single word
    @Override
    public int calculateWordScore(String word, boolean isMielegramma) {
        if (word == null || word.trim().length() < MIN_WORD_LENGTH) {
            return 0;
        }

        int basePoints = word.length() == MIN_WORD_LENGTH ? 1 : word.length();
        int bonus = isMielegramma ? MIELEGRAMMA_BONUS : 0;

        return basePoints + bonus;
    }

    // Calculates the current completion percentage and returns the session's
    // RankTier
    @Override
    public RankTier calculateCurrentRank(int currentScore, int maxScore) {
        if (maxScore <= 0) {
            return RankTier.INITIAL;
        }

        double percentage = ((double) currentScore / maxScore) * 100.0;
        return RankTier.getRankForPercentage(percentage);
    }

    @Override
    public CareerTier calculateCareerTier(int totalSeasonalPoints, int annualTargetPoints) {
        if (annualTargetPoints <= 0) {
            return CareerTier.EGG;
        }

        double percentage = ((double) totalSeasonalPoints / annualTargetPoints) * 100.0;
        return CareerTier.getTierForPercentage(percentage);
    }

    // Checks if a word is a Mielegramma (contains exactly all 7 unique letters of
    // the grid)
    @Override
    public boolean isMielegramma(String word) {
        if (word == null || word.length() < REQUIRED_UNIQUE_LETTERS) {
            return false;
        }
        long uniqueCount = word.toUpperCase().chars().distinct().count();
        return uniqueCount == REQUIRED_UNIQUE_LETTERS;
    }
}