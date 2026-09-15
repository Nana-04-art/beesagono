package com.beesagono.backend.service;

import com.beesagono.backend.enums.CareerTier;
import com.beesagono.backend.enums.RankTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceImplTest {

    private ScoringServiceImpl scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringServiceImpl();
    }

    @Nested
    @DisplayName("calculateWordScore Tests")
    class CalculateWordScoreTests {

        @Test
        @DisplayName("Should return 0 for null, empty or words shorter than 4 letters")
        void shouldReturnZeroForInvalidWordLength() {
            assertThat(scoringService.calculateWordScore(null, false)).isZero();
            assertThat(scoringService.calculateWordScore("", false)).isZero();
            assertThat(scoringService.calculateWordScore("  ", false)).isZero();
            assertThat(scoringService.calculateWordScore("APE", false)).isZero();
        }

        @Test
        @DisplayName("Should return 1 point for 4-letter standard words")
        void shouldReturnOnePointForFourLetterWord() {
            assertThat(scoringService.calculateWordScore("CASA", false)).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return length as points for words longer than 4 letters")
        void shouldReturnLengthForLongerWords() {
            assertThat(scoringService.calculateWordScore("AMICO", false)).isEqualTo(5);
            assertThat(scoringService.calculateWordScore("ABITARE", false)).isEqualTo(7);
        }

        @Test
        @DisplayName("Should add +7 mielegramma bonus to 4-letter words")
        void shouldAddBonusToFourLetterMielegramma() {
            assertThat(scoringService.calculateWordScore("CASA", true)).isEqualTo(8); // 1 + 7
        }

        @Test
        @DisplayName("Should add +7 mielegramma bonus to longer words")
        void shouldAddBonusToLongerMielegramma() {
            assertThat(scoringService.calculateWordScore("ALBERGO", true)).isEqualTo(14); // 7 + 7
        }
    }

    @Nested
    @DisplayName("calculateCurrentRank Tests")
    class CalculateCurrentRankTests {

        @Test
        @DisplayName("Should return INITIAL rank when maxScore is <= 0")
        void shouldReturnInitialWhenMaxScoreInvalid() {
            assertThat(scoringService.calculateCurrentRank(50, 0)).isEqualTo(RankTier.INITIAL);
            assertThat(scoringService.calculateCurrentRank(50, -10)).isEqualTo(RankTier.INITIAL);
        }

        @Test
        @DisplayName("Should calculate correct rank based on percentage thresholds")
        void shouldCalculateCorrectRank() {
            int maxScore = 100;

            assertThat(scoringService.calculateCurrentRank(0, maxScore)).isEqualTo(RankTier.INITIAL);
            assertThat(scoringService.calculateCurrentRank(2, maxScore)).isEqualTo(RankTier.FRESH_MIND);
            assertThat(scoringService.calculateCurrentRank(5, maxScore)).isEqualTo(RankTier.BEGINNER);
            assertThat(scoringService.calculateCurrentRank(15, maxScore)).isEqualTo(RankTier.EXPERT);
            assertThat(scoringService.calculateCurrentRank(40, maxScore)).isEqualTo(RankTier.GENIUS);
            assertThat(scoringService.calculateCurrentRank(100, maxScore)).isEqualTo(RankTier.QUEEN_BEE);
        }
    }

    @Nested
    @DisplayName("calculateCareerTier Tests")
    class CalculateCareerTierTests {

        @Test
        @DisplayName("Should return EGG tier when annualTargetPoints is <= 0")
        void shouldReturnEggWhenTargetInvalid() {
            assertThat(scoringService.calculateCareerTier(100, 0)).isEqualTo(CareerTier.EGG);
            assertThat(scoringService.calculateCareerTier(100, -1)).isEqualTo(CareerTier.EGG);
        }

        @Test
        @DisplayName("Should calculate correct CareerTier for all percentage thresholds")
        void shouldCalculateCorrectCareerTier() {
            int targetPoints = 100;

            assertThat(scoringService.calculateCareerTier(0, targetPoints)).isEqualTo(CareerTier.EGG); // 0%
            assertThat(scoringService.calculateCareerTier(14, targetPoints)).isEqualTo(CareerTier.EGG); // < 15%
            assertThat(scoringService.calculateCareerTier(15, targetPoints)).isEqualTo(CareerTier.LARVA); // 15%
            assertThat(scoringService.calculateCareerTier(30, targetPoints)).isEqualTo(CareerTier.NURSE_BEE); // 30%
            assertThat(scoringService.calculateCareerTier(45, targetPoints)).isEqualTo(CareerTier.WORKER_BEE); // 45%
            assertThat(scoringService.calculateCareerTier(60, targetPoints)).isEqualTo(CareerTier.FORAGER_BEE); // 60%
            assertThat(scoringService.calculateCareerTier(75, targetPoints)).isEqualTo(CareerTier.GUARDIAN_BEE); // 75%
            assertThat(scoringService.calculateCareerTier(85, targetPoints)).isEqualTo(CareerTier.DEFENDER_BEE); // 85%
            assertThat(scoringService.calculateCareerTier(95, targetPoints)).isEqualTo(CareerTier.ARCHITECT_BEE);// 95%
            assertThat(scoringService.calculateCareerTier(100, targetPoints)).isEqualTo(CareerTier.SEASON_QUEEN);// 100%
        }
    }

    @Nested
    @DisplayName("isMielegramma Tests")
    class IsMielegrammaTests {

        @Test
        @DisplayName("Should return false for null or words shorter than 7 letters")
        void shouldReturnFalseForShortOrNullWords() {
            assertThat(scoringService.isMielegramma(null)).isFalse();
            assertThat(scoringService.isMielegramma("CASA")).isFalse();
            assertThat(scoringService.isMielegramma("ALBERO")).isFalse(); // 6 letters
        }

        @Test
        @DisplayName("Should return false if 7+ letter word has fewer than 7 unique letters")
        void shouldReturnFalseForWordWithRepeatedLettersNotReachingSevenUnique() {
            assertThat(scoringService.isMielegramma("AABBCCD")).isFalse(); // only 4 unique letters
        }

        @Test
        @DisplayName("Should return true for word containing exactly 7 unique letters")
        void shouldReturnTrueForValidMielegramma() {
            assertThat(scoringService.isMielegramma("ALBERGO")).isTrue(); // A, L, B, E, R, G, O (7 uniques)
            assertThat(scoringService.isMielegramma("albergo")).isTrue(); // Case-insensitive
        }
    }
}