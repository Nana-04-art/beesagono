package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class GameSessionRepositoryTest {

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = entityManager.persist(createUser("player1", "player1@example.com"));
    }

    // --- findByUserIdAndPuzzleId ---

    @Test
    @DisplayName("findByUserIdAndPuzzleId - Should find session by userId and puzzleId")
    void shouldFindByUserIdAndPuzzleId() {
        DailyPuzzle puzzle = entityManager.persist(createDailyPuzzle(LocalDate.now(), "A", 100, "seed1"));
        GameSession session = entityManager.persistAndFlush(createGameSession(defaultUser, puzzle, 10, "Beginner", false));

        Optional<GameSession> found = gameSessionRepository.findByUserIdAndPuzzleId(defaultUser.getId(), puzzle.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(session.getId());
    }

    @Test
    @DisplayName("findByUserIdAndPuzzleId - Should return empty Optional when session not found")
    void shouldReturnEmptyWhenSessionNotFound() {
        Optional<GameSession> found = gameSessionRepository.findByUserIdAndPuzzleId("fake-user", "fake-puzzle");

        assertThat(found).isEmpty();
    }

    // --- findByUserId ---

    @Test
    @DisplayName("findByUserId - Should find all sessions by userId")
    void shouldFindByUserId() {
        DailyPuzzle puzzle = entityManager.persist(createDailyPuzzle(LocalDate.now(), "B", 80, "seed2"));
        entityManager.persistAndFlush(createGameSession(defaultUser, puzzle, 20, "Good", false));

        List<GameSession> sessions = gameSessionRepository.findByUserId(defaultUser.getId());

        assertThat(sessions).hasSize(1);
    }

    @Test
    @DisplayName("findByUserId - Should return empty list when user has no sessions")
    void shouldReturnEmptyWhenUserHasNoSessions() {
        List<GameSession> sessions = gameSessionRepository.findByUserId("nonexistent-user");

        assertThat(sessions).isEmpty();
    }

    // --- findByUserIdAndPuzzlePuzzleDate ---

    @Test
    @DisplayName("findByUserIdAndPuzzlePuzzleDate - Should find session by userId and puzzleDate")
    void shouldFindByUserIdAndPuzzlePuzzleDate() {
        LocalDate today = LocalDate.now();
        DailyPuzzle puzzle = entityManager.persist(createDailyPuzzle(today, "C", 120, "seed3"));
        entityManager.persistAndFlush(createGameSession(defaultUser, puzzle, 30, "Great", false));

        Optional<GameSession> found = gameSessionRepository.findByUserIdAndPuzzlePuzzleDate(defaultUser.getId(), today);

        assertThat(found).isPresent();
        assertThat(found.get().getPuzzle().getPuzzleDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("findByUserIdAndPuzzlePuzzleDate - Should return empty Optional when date does not match")
    void shouldReturnEmptyWhenPuzzleDateMismatch() {
        Optional<GameSession> found = gameSessionRepository.findByUserIdAndPuzzlePuzzleDate(
                defaultUser.getId(), 
                LocalDate.of(2099, 1, 1)
        );

        assertThat(found).isEmpty();
    }

    // --- existsByUserIdAndPuzzleId ---

    @Test
    @DisplayName("existsByUserIdAndPuzzleId - Should return true when session exists")
    void shouldReturnTrueWhenExistsByUserIdAndPuzzleId() {
        DailyPuzzle puzzle = entityManager.persist(createDailyPuzzle(LocalDate.now(), "D", 90, "seed4"));
        entityManager.persistAndFlush(createGameSession(defaultUser, puzzle, 5, "Beginner", false));

        boolean exists = gameSessionRepository.existsByUserIdAndPuzzleId(defaultUser.getId(), puzzle.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndPuzzleId - Should return false when session does not exist")
    void shouldReturnFalseWhenNotExistsByUserIdAndPuzzleId() {
        boolean exists = gameSessionRepository.existsByUserIdAndPuzzleId(defaultUser.getId(), "fake-puzzle");

        assertThat(exists).isFalse();
    }

    // --- countByPuzzleIdAndIsCompletedFalse ---

    @Test
    @DisplayName("countByPuzzleIdAndIsCompletedFalse - Should count active (in-progress) sessions for a puzzle")
    void shouldCountActiveSessionsForPuzzle() {
        DailyPuzzle puzzle = entityManager.persist(createDailyPuzzle(LocalDate.now(), "E", 150, "seed5"));
        User secondUser = entityManager.persist(createUser("player2", "p2@ex.com"));

        entityManager.persist(createGameSession(defaultUser, puzzle, 10, "Beginner", false));
        entityManager.persist(createGameSession(secondUser, puzzle, 150, "Genius", true));
        entityManager.flush();

        Long activeCount = gameSessionRepository.countByPuzzleIdAndIsCompletedFalse(puzzle.getId());

        assertThat(activeCount).isEqualTo(1L);
    }

    // --- countByPuzzleIdAndIsCompletedTrue ---

    @Test
    @DisplayName("countByPuzzleIdAndIsCompletedTrue - Should count completed sessions for a puzzle")
    void shouldCountCompletedSessionsForPuzzle() {
        DailyPuzzle puzzle = entityManager.persist(createDailyPuzzle(LocalDate.now(), "F", 150, "seed6"));
        User secondUser = entityManager.persist(createUser("player3", "p3@ex.com"));

        entityManager.persist(createGameSession(defaultUser, puzzle, 150, "Genius", true));
        entityManager.persist(createGameSession(secondUser, puzzle, 150, "Genius", true));
        entityManager.flush();

        Long completedCount = gameSessionRepository.countByPuzzleIdAndIsCompletedTrue(puzzle.getId());

        assertThat(completedCount).isEqualTo(2L);
    }

    // --- Helper Methods ---

    private User createUser(String username, String email) {
        return User.builder()
                .username(username)
                .email(email)
                .passwordHash("pwd")
                .build();
    }

    private DailyPuzzle createDailyPuzzle(LocalDate date, String centerLetter, int maxScore, String seed) {
        return DailyPuzzle.builder()
                .puzzleDate(date)
                .centerLetter(centerLetter)
                .maxScore(maxScore)
                .seed(seed)
                .build();
    }

    private GameSession createGameSession(User user, DailyPuzzle puzzle, int score, String rank, boolean isCompleted) {
        return GameSession.builder()
                .user(user)
                .puzzle(puzzle)
                .currentScore(score)
                .currentRankLabel(rank)
                .isCompleted(isCompleted)
                .startTime(new Date())
                .build();
    }
}