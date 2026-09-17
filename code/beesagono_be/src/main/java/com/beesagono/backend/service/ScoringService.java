package com.beesagono.backend.service;

import com.beesagono.backend.enums.CareerTier;
import com.beesagono.backend.enums.RankTier;

/**
 * Utility service interface defining business rules for word validation scoring,
 * daily rank tier assignments, and seasonal career tier calculations.
 */
public interface ScoringService {

    /**
     * Calculates base score points awarded for a validated word submission.
     *
     * @param word           the submitted word string
     * @param isMielegramma flag indicating if the word is a pangram utilizing all puzzle letters
     * @return calculated point score
     */
    int calculateWordScore(String word, boolean isMielegramma);

    /**
     * Calculates current daily rank tier based on achieved score percentage relative to maximum possible puzzle score.
     *
     * @param currentScore points accumulated in current session
     * @param maxScore     total maximum obtainable score for current puzzle
     * @return matching {@link RankTier}
     */
    RankTier calculateCurrentRank(int currentScore, int maxScore);

    /**
     * Determines player seasonal career tier based on accumulated seasonal points.
     *
     * @param totalSeasonalPoints current total accumulated points in active season
     * @param annualTargetPoints  annual benchmark point target
     * @return matching {@link CareerTier}
     */
    CareerTier calculateCareerTier(int totalSeasonalPoints, int annualTargetPoints);

    /**
     * Checks whether a word qualifies as a mielegramma (pangram) for the given letter set.
     *
     * @param word candidate word string
     * @return {@code true} if word uses all 7 unique puzzle letters, {@code false} otherwise
     */
    boolean isMielegramma(String word);
}