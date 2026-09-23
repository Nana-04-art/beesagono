package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.FoundWord;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.FoundWordId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class FoundWordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FoundWordRepository foundWordRepository;

    private User user1;
    private User user2;
    private GameSession session1;
    private GameSession session2;

    @BeforeEach
    void setUp() {
        user1 = entityManager.persist(User.builder()
                .username("player1")
                .email("player1@example.com")
                .passwordHash("hashed_pwd_1")
                .build());

        user2 = entityManager.persist(User.builder()
                .username("player2")
                .email("player2@example.com")
                .passwordHash("hashed_pwd_2")
                .build());

        DailyPuzzle puzzle = entityManager.persist(DailyPuzzle.builder()
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(100)
                .seed("seed-123")
                .build());

        session1 = entityManager.persist(GameSession.builder()
                .user(user1)
                .puzzle(puzzle)
                .currentScore(10)
                .currentRankLabel("Principiante")
                .isCompleted(false)
                .build());

        session2 = entityManager.persist(GameSession.builder()
                .user(user2)
                .puzzle(puzzle)
                .currentScore(5)
                .currentRankLabel("Principiante")
                .isCompleted(false)
                .build());

        FoundWord word1 = FoundWord.builder()
                .id(new FoundWordId(session1.getId(), "CASA"))
                .session(session1)
                .scoreAssigned(4)
                .isMielegramma(false)
                .build();

        FoundWord word2 = FoundWord.builder()
                .id(new FoundWordId(session1.getId(), "ALBERO"))
                .session(session1)
                .scoreAssigned(6)
                .isMielegramma(true) // Pangram/Mielegramma
                .build();

        FoundWord word3 = FoundWord.builder()
                .id(new FoundWordId(session2.getId(), "CASA"))
                .session(session2)
                .scoreAssigned(4)
                .isMielegramma(false)
                .build();

        entityManager.persist(word1);
        entityManager.persist(word2);
        entityManager.persist(word3);
        entityManager.flush();
    }

    // --- findByIdSessionId ---

    @Nested
    @DisplayName("findByIdSessionId Tests")
    class FindByIdSessionIdTests {

        @Test
        @DisplayName("findByIdSessionId - Success when words exist for session")
        void findByIdSessionId_ShouldReturnWords_WhenSessionExists() {
            List<FoundWord> result = foundWordRepository.findByIdSessionId(session1.getId());

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(word -> word.getId().getWord())
                    .containsExactlyInAnyOrder("CASA", "ALBERO");
        }

        @Test
        @DisplayName("findByIdSessionId - Returns empty list when session has no words")
        void findByIdSessionId_ShouldReturnEmpty_WhenSessionHasNoWords() {
            List<FoundWord> result = foundWordRepository.findByIdSessionId("non-existent-session-id");

            assertThat(result).isEmpty();
        }
    }

    // --- existsByIdSessionIdAndIdWord ---

    @Nested
    @DisplayName("existsByIdSessionIdAndIdWord Tests")
    class ExistsByIdSessionIdAndIdWordTests {

        @Test
        @DisplayName("existsByIdSessionIdAndIdWord - Returns true when word exists in session")
        void existsByIdSessionIdAndIdWord_ShouldReturnTrue_WhenWordExistsInSession() {
            boolean exists = foundWordRepository.existsByIdSessionIdAndIdWord(session1.getId(), "CASA");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsByIdSessionIdAndIdWord - Returns false when word does not exist in session")
        void existsByIdSessionIdAndIdWord_ShouldReturnFalse_WhenWordDoesNotExistInSession() {
            boolean existsInSession1 = foundWordRepository.existsByIdSessionIdAndIdWord(session1.getId(), "GATTO");
            boolean existsInSession2 = foundWordRepository.existsByIdSessionIdAndIdWord(session2.getId(), "ALBERO");

            assertThat(existsInSession1).isFalse();
            assertThat(existsInSession2).isFalse();
        }
    }

    // --- findById ---

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("findById - Success using composite primary key FoundWordId")
        void findById_ShouldReturnWord_WhenCompositeKeyExists() {
            FoundWordId id = new FoundWordId(session1.getId(), "CASA");

            Optional<FoundWord> result = foundWordRepository.findById(id);

            assertThat(result).isPresent();
            assertThat(result.get().getScoreAssigned()).isEqualTo(4);
            assertThat(result.get().getSession().getId()).isEqualTo(session1.getId());
        }
    }

    // --- Badge Counting & Query Tests ---

    @Nested
    @DisplayName("Badge Counting & Native/JPQL Query Tests")
    class BadgeQueryTests {

        @Test
        @DisplayName("countBySessionUserId - Should count total words found by user")
        void shouldCountWordsByUserId() {
            long countUser1 = foundWordRepository.countBySessionUserId(user1.getId());
            long countUser2 = foundWordRepository.countBySessionUserId(user2.getId());

            assertThat(countUser1).isEqualTo(2L);
            assertThat(countUser2).isEqualTo(1L);
        }

        @Test
        @DisplayName("countPangramsByUserId - Should count only words flagged as mielegramma")
        void shouldCountPangramsByUserId() {
            long pangramsUser1 = foundWordRepository.countPangramsByUserId(user1.getId());
            long pangramsUser2 = foundWordRepository.countPangramsByUserId(user2.getId());

            assertThat(pangramsUser1).isEqualTo(1L);
            assertThat(pangramsUser2).isEqualTo(0L);
        }

        @Test
        @DisplayName("findFirstWordDateByUserId - Should return startTime of earliest session with found word")
        void shouldFindFirstWordDateByUserId() {
            Optional<Date> firstWordDate = foundWordRepository.findFirstWordDateByUserId(user1.getId());

            assertThat(firstWordDate).isPresent();
            assertThat(firstWordDate.get()).isNotNull();
        }

        @Test
        @DisplayName("findFirstPangramDateByUserId - Should return startTime of earliest session with pangram")
        void shouldFindFirstPangramDateByUserId() {
            Optional<Date> firstPangramDate = foundWordRepository.findFirstPangramDateByUserId(user1.getId());

            assertThat(firstPangramDate).isPresent();
            assertThat(firstPangramDate.get()).isNotNull();
        }

        @Test
        @DisplayName("findNthWordDateByUserId - Should return date with offset using native query")
        void shouldFindNthWordDateByUserId() {
            Optional<?> nthWordDate = foundWordRepository.findNthWordDateByUserId(user1.getId(), 0);

            assertThat(nthWordDate).isPresent();
            assertThat(nthWordDate.get()).isNotNull();
        }

        @Test
        @DisplayName("findNthPangramDateByUserId - Should return date with offset for pangrams using native query")
        void shouldFindNthPangramDateByUserId() {
            Optional<?> nthPangramDate = foundWordRepository.findNthPangramDateByUserId(user1.getId(), 0);

            assertThat(nthPangramDate).isPresent();
            assertThat(nthPangramDate.get()).isNotNull();
        }
    }
}