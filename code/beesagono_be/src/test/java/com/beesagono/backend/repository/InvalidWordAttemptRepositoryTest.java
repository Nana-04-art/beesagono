package com.beesagono.backend.repository;

import com.beesagono.backend.entity.*;
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

                ErrorType errorType = entityManager.persist(ErrorType.builder()
                                .code(ErrorTypeCode.TOO_SHORT)
                                .description("Too short")
                                .build());

                entityManager.persistAndFlush(InvalidWordAttempt.builder()
                                .session(session)
                                .attemptedWord("SOL")
                                .errorReason(errorType)
                                .build());

                List<InvalidWordAttempt> attempts = invalidWordAttemptRepository.findBySessionId(session.getId());

                assertThat(attempts).hasSize(1);
                assertThat(attempts.get(0).getAttemptedWord()).isEqualTo("SOL");
        }
}