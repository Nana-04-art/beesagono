package com.beesagono.backend.repository;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.testsupport.H2DataJpaTest;
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

        @Test
        @DisplayName("findByUserIdAndPuzzleId - Should find session by userId and puzzleId")
        void shouldFindByUserIdAndPuzzleId() {
                User user = entityManager.persist(User.builder()
                                .username("player1")
                                .email("player1@example.com")
                                .passwordHash("pwd")
                                .build());

                DailyPuzzle puzzle = entityManager.persist(DailyPuzzle.builder()
                                .puzzleDate(LocalDate.now())
                                .centerLetter("A")
                                .maxScore(100)
                                .seed("seed1")
                                .build());

                GameSession session = entityManager.persist(GameSession.builder()
                                .user(user)
                                .puzzle(puzzle)
                                .currentScore(10)
                                .currentRankLabel("Beginner")
                                .startTime(new Date())
                                .build());

                Optional<GameSession> found = gameSessionRepository.findByUserIdAndPuzzleId(user.getId(),
                                puzzle.getId());

                assertThat(found).isPresent();
                assertThat(found.get().getId()).isEqualTo(session.getId());
        }

        @Test
        @DisplayName("findByUserId - Should find all sessions by userId")
        void shouldFindByUserId() {
                User user = entityManager.persist(User.builder()
                                .username("player2")
                                .email("player2@example.com")
                                .passwordHash("pwd")
                                .build());

                DailyPuzzle puzzle = entityManager.persist(DailyPuzzle.builder()
                                .puzzleDate(LocalDate.now())
                                .centerLetter("B")
                                .maxScore(80)
                                .seed("seed2")
                                .build());

                entityManager.persist(GameSession.builder()
                                .user(user)
                                .puzzle(puzzle)
                                .currentScore(20)
                                .currentRankLabel("Good")
                                .startTime(new Date())
                                .build());

                List<GameSession> sessions = gameSessionRepository.findByUserId(user.getId());

                assertThat(sessions).hasSize(1);
        }
}