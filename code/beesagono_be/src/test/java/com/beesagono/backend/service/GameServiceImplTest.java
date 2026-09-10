package com.beesagono.backend.service;

import com.beesagono.backend.dto.game.GameBulkSyncRequest;
import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.dto.game.GameSyncRequest;
import com.beesagono.backend.dto.game.SubmitWordRequest;
import com.beesagono.backend.dto.game.SubmitWordResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.FoundWord;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.InvalidWordAttempt;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.FoundWordId;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.enums.RankTier;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.FoundWordRepository;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.InvalidWordAttemptRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import com.beesagono.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @Mock
    private GameSessionRepository gameSessionRepository;
    @Mock
    private FoundWordRepository foundWordRepository;
    @Mock
    private DailyPuzzleRepository dailyPuzzleRepository;
    @Mock
    private PuzzleWordRepository puzzleWordRepository;
    @Mock
    private InvalidWordAttemptRepository invalidWordAttemptRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DictionaryWordRepository dictionaryRepository;
    @Mock
    private PuzzleGeneratorService puzzleService;

    @InjectMocks
    private GameServiceImpl gameService;

    private User user;
    private DailyPuzzle puzzle;
    private GameSession session;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user-123")
                .username("player1")
                .email("player1@example.com")
                .build();

        puzzle = DailyPuzzle.builder()
                .id("puzzle-123")
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .maxScore(100)
                .build();

        session = GameSession.builder()
                .id("session-123")
                .user(user)
                .puzzle(puzzle)
                .currentScore(0)
                .currentRankLabel(RankTier.BEGINNER.getLabel())
                .isCompleted(false)
                .startTime(new Date())
                .lastUpdated(new Date())
                .build();
    }

    @Nested
    @DisplayName("getOrCreateTodaySession Tests")
    class GetOrCreateTodaySessionTests {

        @Test
        @DisplayName("Should return existing session when session already exists for today")
        void shouldReturnExistingSession() {
            LocalDate today = LocalDate.now();

            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(puzzle));
            when(gameSessionRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()))
                    .thenReturn(Optional.of(session));
            when(foundWordRepository.findByIdSessionId(session.getId())).thenReturn(Collections.emptyList());

            GameSessionResponse response = gameService.getOrCreateTodaySession(user.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(session.getId());
            assertThat(response.getUserId()).isEqualTo(user.getId());
            assertThat(response.getPuzzleId()).isEqualTo(puzzle.getId());

            verify(puzzleService, times(1)).generateAndSavePuzzleForDate(today);
            verify(userRepository, never()).findById(any());
            verify(gameSessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create and return new session when session does not exist")
        void shouldCreateNewSessionWhenNotFound() {
            LocalDate today = LocalDate.now();

            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(puzzle));
            when(gameSessionRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);
            when(foundWordRepository.findByIdSessionId(session.getId())).thenReturn(Collections.emptyList());

            GameSessionResponse response = gameService.getOrCreateTodaySession(user.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(session.getId());

            verify(puzzleService, times(1)).generateAndSavePuzzleForDate(today);
            verify(userRepository, times(1)).findById(user.getId());
            verify(gameSessionRepository, times(1)).save(any(GameSession.class));
        }

        @Test
        @DisplayName("Should throw exception when puzzle is not found after generation")
        void shouldThrowExceptionWhenPuzzleNotFound() {
            LocalDate today = LocalDate.now();
            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.getOrCreateTodaySession(user.getId()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Puzzle per la data " + today + " non trovato.");
        }
    }
    
    @Nested
    @DisplayName("validateAndScoreWord Tests")
    class ValidateAndScoreWordTests {

        @Test
        @DisplayName("Should throw exception when session does not exist")
        void shouldThrowExceptionWhenSessionNotFound() {
            SubmitWordRequest request = new SubmitWordRequest("non-existent-session", "CASA");

            when(gameSessionRepository.findById(request.getSessionId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.validateAndScoreWord(request, user.getId()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Sessione di gioco non trovata");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user is unauthorized")
        void shouldThrowExceptionWhenUserUnauthorized() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "CASA");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> gameService.validateAndScoreWord(request, "different-user-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Non sei autorizzato");
        }

        @Test
        @DisplayName("Should fail when word is too short (< 4 chars)")
        void shouldFailWhenWordIsTooShort() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "APA");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            SubmitWordResponse response = gameService.validateAndScoreWord(request, user.getId());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.TOO_SHORT);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }

        @Test
        @DisplayName("Should fail when word misses center letter")
        void shouldFailWhenWordMissesCenterLetter() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "EDILIZIA"); // Doesn't contain 'A'
                                                                                            // (center is 'A')

            session.getPuzzle().setCenterLetter("X");
            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            SubmitWordResponse response = gameService.validateAndScoreWord(request, user.getId());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.MISSING_CENTER);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }

        @Test
        @DisplayName("Should fail when word is already found in session")
        void shouldFailWhenWordAlreadyFound() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "CASA");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "CASA")).thenReturn(true);

            SubmitWordResponse response = gameService.validateAndScoreWord(request, user.getId());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.ALREADY_FOUND);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }

        @Test
        @DisplayName("Should score standard 4-letter word successfully (1 point)")
        void shouldScoreStandardFourLetterWord() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "casa");
            PuzzleWord puzzleWord = PuzzleWord.builder().isMielegramma(false).build();

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "CASA")).thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "CASA"))
                    .thenReturn(Optional.of(puzzleWord));

            SubmitWordResponse response = gameService.validateAndScoreWord(request, user.getId());

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getWord()).isEqualTo("CASA");
            assertThat(response.getPointsEarned()).isEqualTo(1);
            assertThat(response.getCurrentScore()).isEqualTo(1);
            assertThat(response.isMielegramma()).isFalse();

            verify(foundWordRepository, times(1)).save(any(FoundWord.class));
            verify(gameSessionRepository, times(1)).save(session);
        }

        @Test
        @DisplayName("Should score mielegramma word successfully with bonus (+7 points)")
        void shouldScoreMielegrammaWord() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "ALBERGO");
            PuzzleWord puzzleWord = PuzzleWord.builder().isMielegramma(true).build();

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "ALBERGO")).thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "ALBERGO"))
                    .thenReturn(Optional.of(puzzleWord));

            SubmitWordResponse response = gameService.validateAndScoreWord(request, user.getId());

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getPointsEarned()).isEqualTo(14); // 7 length + 7 bonus
            assertThat(response.isMielegramma()).isTrue();
        }

        @Test
        @DisplayName("Should fail with NOT_IN_PUZZLE when word is valid in dictionary but not in puzzle")
        void shouldFailWhenWordNotInPuzzleButInDictionary() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "ALBERO");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "ALBERO")).thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "ALBERO")).thenReturn(Optional.empty());
            when(dictionaryRepository.existsByWord("ALBERO")).thenReturn(true);

            SubmitWordResponse response = gameService.validateAndScoreWord(request, user.getId());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.NOT_IN_PUZZLE);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }

        @Test
        @DisplayName("Should fail with NOT_IN_DICTIONARY when word is not in dictionary")
        void shouldFailWhenWordNotInDictionary() {
            // We use "ZZZA" because it contains the central letter 'A' and has a length of
            // 4 or greater
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "ZZZA");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "ZZZA")).thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "ZZZA")).thenReturn(Optional.empty());
            when(dictionaryRepository.existsByWord("ZZZA")).thenReturn(false);

            SubmitWordResponse response = gameService.validateAndScoreWord(request, user.getId());

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.NOT_IN_DICTIONARY);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }
    }

    @Nested
    @DisplayName("syncLocalProgress Tests")
    class SyncLocalProgressTests {

        @Test
        @DisplayName("Should return empty list when request games list is empty")
        void shouldReturnEmptyWhenRequestIsEmpty() {
            GameBulkSyncRequest request = new GameBulkSyncRequest(Collections.emptyList());

            List<GameSessionResponse> responses = gameService.syncLocalProgress(request, user.getId());

            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("Should sync local progress and persist new found words")
        void shouldSyncLocalProgressSuccessfully() {
            LocalDate puzzleDate = LocalDate.now();
            GameSyncRequest syncRequest = new GameSyncRequest();
            syncRequest.setPuzzleDate(puzzleDate);
            syncRequest.setCenterLetter("A");
            syncRequest.setFoundWords(List.of("CASA", "ALBERO"));

            GameBulkSyncRequest request = new GameBulkSyncRequest(List.of(syncRequest));
            PuzzleWord pw1 = PuzzleWord.builder().isMielegramma(false).build();
            PuzzleWord pw2 = PuzzleWord.builder().isMielegramma(true).build();

            when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));
            when(gameSessionRepository.findByUserIdAndPuzzleId(user.getId(), puzzle.getId()))
                    .thenReturn(Optional.of(session));

            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "CASA")).thenReturn(false);
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "ALBERO")).thenReturn(false);

            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "CASA")).thenReturn(Optional.of(pw1));
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "ALBERO")).thenReturn(Optional.of(pw2));

            FoundWord fw1 = FoundWord.builder().id(new FoundWordId(session.getId(), "CASA")).build();
            FoundWord fw2 = FoundWord.builder().id(new FoundWordId(session.getId(), "ALBERO")).build();
            when(foundWordRepository.findByIdSessionId(session.getId())).thenReturn(List.of(fw1, fw2));

            List<GameSessionResponse> responses = gameService.syncLocalProgress(request, user.getId());

            assertThat(responses).hasSize(1);
            GameSessionResponse response = responses.get(0);
            assertThat(response.getFoundWords()).containsExactlyInAnyOrder("CASA", "ALBERO");

            verify(foundWordRepository, times(2)).save(any(FoundWord.class));
            verify(gameSessionRepository, times(1)).save(session);
        }

        @Test
        @DisplayName("Should ignore sync item if center letter mismatch occurs")
        void shouldIgnoreSyncItemWhenCenterLetterMismatches() {
            LocalDate puzzleDate = LocalDate.now();
            GameSyncRequest syncRequest = new GameSyncRequest();
            syncRequest.setPuzzleDate(puzzleDate);
            syncRequest.setCenterLetter("Z"); // Mismatch: Puzzle has 'A'
            syncRequest.setFoundWords(List.of("CASA"));

            GameBulkSyncRequest request = new GameBulkSyncRequest(List.of(syncRequest));

            when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));

            List<GameSessionResponse> responses = gameService.syncLocalProgress(request, user.getId());

            assertThat(responses).isEmpty();
            verify(gameSessionRepository, never()).findByUserIdAndPuzzleId(any(), any());
        }
    }
}