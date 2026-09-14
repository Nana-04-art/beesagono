package com.beesagono.backend.repository;

import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStat;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.InvalidWordAttempt;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.testsupport.H2DataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@H2DataJpaTest
class InvalidWordAttemptRepositoryTest {

    @Autowired
    private InvalidWordAttemptRepository invalidWordAttemptRepository;

    @Autowired
    private TestEntityManager entityManager;

    private GameSession defaultSession;

    @BeforeEach
    void setUp() {
        User user = entityManager.persist(createUser("tester", "tester@example.com"));
        DailyPuzzle puzzle = entityManager.persist(createDailyPuzzle(LocalDate.now(), "C", 50, "seed3"));
        defaultSession = entityManager.persist(createGameSession(user, puzzle));
    }

    // --- findBySessionId ---

    @Test
    @DisplayName("findBySessionId - Should return list of invalid word attempts for a given sessionId")
    void shouldFindBySessionId() {
        InvalidWordAttempt attempt = createInvalidAttempt(defaultSession, "SOL", ErrorTypeCode.TOO_SHORT);
        entityManager.persistAndFlush(attempt);

        List<InvalidWordAttempt> attempts = invalidWordAttemptRepository.findBySessionId(defaultSession.getId());

        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getAttemptedWord()).isEqualTo("SOL");
        assertThat(attempts.get(0).getErrorReason()).isEqualTo(ErrorTypeCode.TOO_SHORT);
    }

    @Test
    @DisplayName("findBySessionId - Should return empty list when no attempts exist for sessionId")
    void shouldReturnEmptyWhenNoAttemptsFound() {
        List<InvalidWordAttempt> attempts = invalidWordAttemptRepository.findBySessionId("nonexistent-session");

        assertThat(attempts).isEmpty();
    }

    // --- findByErrorReason ---

    @Test
    @DisplayName("findByErrorReason - Should return aggregated stats grouped by attempted word for an error reason")
    void shouldFindByErrorReason() {
        entityManager.persist(createInvalidAttempt(defaultSession, "SOLO", ErrorTypeCode.NOT_IN_DICTIONARY));
        entityManager.persist(createInvalidAttempt(defaultSession, "SOLO", ErrorTypeCode.NOT_IN_DICTIONARY));
        entityManager.persist(createInvalidAttempt(defaultSession, "CASA", ErrorTypeCode.NOT_IN_DICTIONARY));
        entityManager.flush();

        List<InvalidWordAttemptStat> stats = invalidWordAttemptRepository.findByErrorReason(ErrorTypeCode.NOT_IN_DICTIONARY);

        assertThat(stats).isNotEmpty();

        InvalidWordAttemptStat soloStat = stats.stream()
                .filter(stat -> "SOLO".equals(stat.getAttemptedWord()))
                .findFirst()
                .orElse(null);

        assertThat(soloStat).isNotNull();
        assertThat(soloStat.getAttemptCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("findByErrorReason - Should return empty list when no attempts exist for given error reason")
    void shouldReturnEmptyWhenErrorReasonNotFound() {
        List<InvalidWordAttemptStat> stats = invalidWordAttemptRepository.findByErrorReason(ErrorTypeCode.MISSING_CENTER);

        assertThat(stats).isEmpty();
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

    private GameSession createGameSession(User user, DailyPuzzle puzzle) {
        return GameSession.builder()
                .user(user)
                .puzzle(puzzle)
                .currentScore(0)
                .currentRankLabel("Beginner")
                .startTime(new Date())
                .build();
    }

    private InvalidWordAttempt createInvalidAttempt(GameSession session, String word, ErrorTypeCode errorReason) {
        return InvalidWordAttempt.builder()
                .session(session)
                .attemptedWord(word)
                .errorReason(errorReason)
                .build();
    }
}