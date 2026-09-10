package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.FoundWord;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.FoundWordId;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class FoundWordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FoundWordRepository foundWordRepository;

    private String sessionId1;
    private String sessionId2;

    @BeforeEach
    void setUp() {
        User user1 = User.builder()
                .username("player1")
                .email("player1@example.com")
                .passwordHash("hashed_pwd_1")
                .build();
        user1 = entityManager.persistAndFlush(user1);

        User user2 = User.builder()
                .username("player2")
                .email("player2@example.com")
                .passwordHash("hashed_pwd_2")
                .build();
        user2 = entityManager.persistAndFlush(user2);

        DailyPuzzle puzzle = DailyPuzzle.builder()
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(100)
                .seed("seed-123")
                .build();
        puzzle = entityManager.persistAndFlush(puzzle);

        GameSession session1 = GameSession.builder()
                .user(user1)
                .puzzle(puzzle)
                .currentScore(10)
                .currentRankLabel("Principiante")
                .isCompleted(false)
                .build();
        session1 = entityManager.persistAndFlush(session1);
        sessionId1 = session1.getId();

        GameSession session2 = GameSession.builder()
                .user(user2)
                .puzzle(puzzle)
                .currentScore(5)
                .currentRankLabel("Principiante")
                .isCompleted(false)
                .build();
        session2 = entityManager.persistAndFlush(session2);
        sessionId2 = session2.getId();

        FoundWord word1 = FoundWord.builder()
                .id(new FoundWordId(sessionId1, "CASA"))
                .session(session1)
                .scoreAssigned(4)
                .isMielegramma(false)
                .build();

        FoundWord word2 = FoundWord.builder()
                .id(new FoundWordId(sessionId1, "ALBERO"))
                .session(session1)
                .scoreAssigned(6)
                .isMielegramma(false)
                .build();

        FoundWord word3 = FoundWord.builder()
                .id(new FoundWordId(sessionId2, "CASA"))
                .session(session2)
                .scoreAssigned(4)
                .isMielegramma(false)
                .build();

        entityManager.persist(word1);
        entityManager.persist(word2);
        entityManager.persist(word3);
        entityManager.flush();
    }

    // -- findByIdSessionId --
    @Test
    @DisplayName("findByIdSessionId - Success when words exist for session")
    void findByIdSessionId_ShouldReturnWords_WhenSessionExists() {
        List<FoundWord> result = foundWordRepository.findByIdSessionId(sessionId1);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(word -> word.getId().getWord())
                .containsExactlyInAnyOrder("CASA", "ALBERO");
    }

    // -- findByIdSessionId--
    @Test
    @DisplayName("findByIdSessionId - Returns empty list when session has no words")
    void findByIdSessionId_ShouldReturnEmpty_WhenSessionHasNoWords() {
        List<FoundWord> result = foundWordRepository.findByIdSessionId("non-existent-session-id");

        assertThat(result).isEmpty();
    }

    // -- existsByIdSessionIdAndIdWord --
    @Test
    @DisplayName("existsByIdSessionIdAndIdWord - Returns true when word exists in session")
    void existsByIdSessionIdAndIdWord_ShouldReturnTrue_WhenWordExistsInSession() {
        boolean exists = foundWordRepository.existsByIdSessionIdAndIdWord(sessionId1, "CASA");

        assertThat(exists).isTrue();
    }

    // -- existsByIdSessionIdAndIdWord --
    @Test
    @DisplayName("existsByIdSessionIdAndIdWord - Returns false when word does not exist in session")
    void existsByIdSessionIdAndIdWord_ShouldReturnFalse_WhenWordDoesNotExistInSession() {
        boolean existsInSession1 = foundWordRepository.existsByIdSessionIdAndIdWord(sessionId1, "GATTO");
        boolean existsInSession2 = foundWordRepository.existsByIdSessionIdAndIdWord(sessionId2, "ALBERO");

        assertThat(existsInSession1).isFalse();
        assertThat(existsInSession2).isFalse();
    }

    // -- findById --
    @Test
    @DisplayName("findById - Success using composite primary key FoundWordId")
    void findById_ShouldReturnWord_WhenCompositeKeyExists() {
        FoundWordId id = new FoundWordId(sessionId1, "CASA");

        Optional<FoundWord> result = foundWordRepository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getScoreAssigned()).isEqualTo(4);
        assertThat(result.get().getSession().getId()).isEqualTo(sessionId1);
    }
}