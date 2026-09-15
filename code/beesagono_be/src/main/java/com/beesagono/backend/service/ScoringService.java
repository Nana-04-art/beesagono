package com.beesagono.backend.service;

import com.beesagono.backend.enums.CareerTier;
import com.beesagono.backend.enums.RankTier;

public interface ScoringService {
    int calculateWordScore(String word, boolean isMielegramma);

    RankTier calculateCurrentRank(int currentScore, int maxScore);

    CareerTier calculateCareerTier(int totalSeasonalPoints, int annualTargetPoints);

    boolean isMielegramma(String word);
}