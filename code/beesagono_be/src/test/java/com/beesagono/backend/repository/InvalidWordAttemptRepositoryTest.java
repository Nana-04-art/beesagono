package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.InvalidWordAttempt;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.testsupport.H2DataJpaTest;
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

        // -- findBySessionId --
        @Test
        @DisplayName("findBySessionId - Should return list of invalid word attempts for a given sessionId")
        void shouldFindBySessionId() {
                User user = entityManager.persist(User.builder()
                                .username("tester")
                                .email("tester@example.com")
                                .passwordHash("pwd")
                                .build());

                DailyPuzzle puzzle = entityManager.persist(DailyPuzzle.builder()
                                .puzzleDate(LocalDate.now())
                                .centerLetter("C")
                                .maxScore(50)
                                .seed("seed3")
                                .build());

                GameSession session = entityManager.persist(GameSession.builder()
                                .user(user)
                                .puzzle(puzzle)
                                .currentScore(0)
                                .currentRankLabel("Beginner")
                                .startTime(new Date())
                                .build());
                entityManager.persistAndFlush(InvalidWordAttempt.builder()
                                .session(session)
                                .attemptedWord("SOL")
                                .errorReason(ErrorTypeCode.TOO_SHORT)
                                .build());

                List<InvalidWordAttempt> attempts = invalidWordAttemptRepository.findBySessionId(session.getId());

                assertThat(attempts).hasSize(1);
                assertThat(attempts.get(0).getAttemptedWord()).isEqualTo("SOL");
                assertThat(attempts.get(0).getErrorReason()).isEqualTo(ErrorTypeCode.TOO_SHORT);
        }

        // -- findBySessionId --
        @Test
        @DisplayName("findBySessionId - Should return empty list when no attempts exist for sessionId")
        void shouldReturnEmptyWhenNoAttemptsFound() {
                List<InvalidWordAttempt> attempts = invalidWordAttemptRepository.findBySessionId("nonexistent-session");

                assertThat(attempts).isEmpty();
        }
}