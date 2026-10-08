package com.beesagono.backend.service;

import com.beesagono.backend.enums.CareerTier;
import com.beesagono.backend.enums.RankTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
        @DisplayName("Should return 0 for null or blank words")
        void shouldReturnZeroForNullOrBlank() {
            assertThat(scoringService.calculateWordScore(null, false)).isEqualTo(0);
            assertThat(scoringService.calculateWordScore("", false)).isEqualTo(0);
            assertThat(scoringService.calculateWordScore("   ", false)).isEqualTo(0);
        }

        @ParameterizedTest
        @ValueSource(strings = { "A", "AB", "ABC" })
        @DisplayName("Should return 0 for words shorter than 4 letters")
        void shouldReturnZeroForShortWords(String word) {
            assertThat(scoringService.calculateWordScore(word, false)).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return 1 point for a standard 4-letter word")
        void shouldScoreFourLetterWord() {
            assertThat(scoringService.calculateWordScore("CASA", false)).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 8 points for a 4-letter mielegramma word (1 + 7 bonus)")
        void shouldScoreFourLetterMielegrammaWord() {
            assertThat(scoringService.calculateWordScore("CASA", true)).isEqualTo(8);
        }

        @ParameterizedTest
        @CsvSource({
                "CINQUE, 6, false",
                "CINQUE, 13, true",
                "SEISETTE, 8, false",
                "SEISETTE, 15, true"
        })
        @DisplayName("Should calculate correct score for words longer than 4 letters")
        void shouldScoreLongerWords(String word, int expectedScore, boolean isMielegramma) {
            assertThat(scoringService.calculateWordScore(word, isMielegramma)).isEqualTo(expectedScore);
        }
    }

    @Nested
    @DisplayName("isMielegramma Tests")
    class IsMielegrammaTests {

        @Test
        @DisplayName("Should return false for null or short words (< 7 chars)")
        void shouldReturnFalseForNullOrShortWords() {
            assertThat(scoringService.isMielegramma(null)).isFalse();
            assertThat(scoringService.isMielegramma("ABCDEF")).isFalse();
        }

        @Test
        @DisplayName("Should return true when word contains exactly 7 unique letters")
        void shouldReturnTrueForSevenUniqueLetters() {
            assertThat(scoringService.isMielegramma("ABCDEFG")).isTrue();
            assertThat(scoringService.isMielegramma("AABBCCDDEEFFGG")).isTrue(); // duplicates ignored
        }

        @Test
        @DisplayName("Should return false when word has fewer or more than 7 unique letters")
        void shouldReturnFalseForInvalidUniqueLetterCount() {
            assertThat(scoringService.isMielegramma("AAAAAAA")).isFalse(); // 1 unique
            assertThat(scoringService.isMielegramma("ABCDEFGH")).isFalse(); // 8 unique
        }

        @Test
        @DisplayName("Should ignore case sensitivity when checking unique letters")
        void shouldIgnoreCase() {
            assertThat(scoringService.isMielegramma("abcdefg")).isTrue();
            assertThat(scoringService.isMielegramma("aAbBcCdDeEfFgG")).isTrue();
        }
    }

    @Nested
    @DisplayName("calculateCurrentRank Tests")
    class CalculateCurrentRankTests {

        @Test
        @DisplayName("Should return INITIAL rank when maxScore is less than or equal to 0")
        void shouldReturnInitialWhenMaxScoreInvalid() {
            assertThat(scoringService.calculateCurrentRank(50, 0)).isEqualTo(RankTier.INITIAL);
            assertThat(scoringService.calculateCurrentRank(50, -10)).isEqualTo(RankTier.INITIAL);
        }

        @Test
        @DisplayName("Should return correct RankTier based on percentage score")
        void shouldReturnRankForValidPercentage() {
            // e.g.: 50 / 100 = 50.0%
            RankTier rank = scoringService.calculateCurrentRank(50, 100);
            assertThat(rank).isNotNull();
            assertThat(rank).isEqualTo(RankTier.getRankForPercentage(50.0));
        }
    }

    @Nested
    @DisplayName("calculateCareerTier Tests")
    class CalculateCareerTierTests {

        @Test
        @DisplayName("Should return EGG tier when annualTargetPoints is less than or equal to 0")
        void shouldReturnEggWhenTargetInvalid() {
            assertThat(scoringService.calculateCareerTier(500, 0)).isEqualTo(CareerTier.EGG);
            assertThat(scoringService.calculateCareerTier(500, -1)).isEqualTo(CareerTier.EGG);
        }

        @Test
        @DisplayName("Should return correct CareerTier based on progress percentage")
        void shouldReturnCareerTierForValidPercentage() {
            // e.g.: 250 / 1000 = 25.0%
            CareerTier tier = scoringService.calculateCareerTier(250, 1000);
            assertThat(tier).isNotNull();
            assertThat(tier).isEqualTo(CareerTier.getTierForPercentage(25.0));
        }
    }
}