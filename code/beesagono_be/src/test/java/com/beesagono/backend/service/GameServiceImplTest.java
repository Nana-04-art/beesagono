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
import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.FoundWordId;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
import com.beesagono.backend.enums.ErrorTypeCode;
import com.beesagono.backend.enums.RankTier;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.FoundWordRepository;
import com.beesagono.backend.repository.GameSessionRepository;
import com.beesagono.backend.repository.InvalidWordAttemptRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import com.beesagono.backend.repository.RankHistogramRepository;
import com.beesagono.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
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
    private RankHistogramRepository rankHistogramRepository;
    @Mock
    private PuzzleGeneratorService puzzleService;
    @Mock
    private ScoringService scoringService;
    @Mock
    private BadgeService badgeService;
    @Mock
    private PlayerSeasonService playerSeasonService;
    @Mock
    private PlayerStatsService playerStatsService;

    @InjectMocks
    private GameServiceImpl gameService;

    @Captor
    private ArgumentCaptor<FoundWord> foundWordCaptor;

    @Captor
    private ArgumentCaptor<InvalidWordAttempt> invalidAttemptCaptor;

    private User user;
    private DailyPuzzle puzzle;
    private GameSession session;
    private String userId;

    private List<PuzzleOuterLetter> buildOuterLetters(String puzzleId, List<String> letters) {
        return letters.stream()
                .map(l -> PuzzleOuterLetter.builder()
                        .id(new PuzzleOuterLetterId(puzzleId, l))
                        .build())
                .toList();
    }

    @BeforeEach
    void setUp() {
        userId = "user-123";

        user = User.builder()
                .id(userId)
                .username("player1")
                .email("player1@example.com")
                .build();

        puzzle = DailyPuzzle.builder()
                .id("puzzle-123")
                .puzzleDate(LocalDate.now())
                .centerLetter("A")
                .outerLetters(buildOuterLetters("puzzle-123", List.of("B", "C", "E", "F", "M", "O")))
                .maxScore(100)
                .build();

        session = GameSession.builder()
                .id("session-123")
                .user(user)
                .puzzle(puzzle)
                .currentScore(0)
                .currentRankLabel(RankTier.INITIAL.getLabel())
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
            when(gameSessionRepository.findByUserIdAndPuzzleId(userId, puzzle.getId()))
                    .thenReturn(Optional.of(session));
            when(foundWordRepository.findByIdSessionId(session.getId())).thenReturn(Collections.emptyList());

            GameSessionResponse response = gameService.getOrCreateTodaySession(userId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(session.getId());
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getPuzzleId()).isEqualTo(puzzle.getId());

            verify(puzzleService, times(1)).generateAndSavePuzzleForDate(today);
            verify(userRepository, never()).findById(any());
            verify(gameSessionRepository, never()).save(any());
            verify(playerSeasonService, never()).updateSeasonProgress(anyString(), anyInt(), anyBoolean());
        }

        @Test
        @DisplayName("Should create and return new session when session does not exist")
        void shouldCreateNewSessionWhenNotFound() {
            LocalDate today = LocalDate.now();

            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(puzzle));
            when(gameSessionRepository.findByUserIdAndPuzzleId(userId, puzzle.getId()))
                    .thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);
            when(foundWordRepository.findByIdSessionId(session.getId())).thenReturn(Collections.emptyList());

            GameSessionResponse response = gameService.getOrCreateTodaySession(userId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(session.getId());

            verify(puzzleService, times(1)).generateAndSavePuzzleForDate(today);
            verify(playerSeasonService, times(1)).updateSeasonProgress(userId, 0, false);
            verify(userRepository, times(1)).findById(userId);
            verify(gameSessionRepository, atLeast(1)).save(any(GameSession.class));
        }

        @Test
        @DisplayName("Should throw exception when puzzle is not found after generation")
        void shouldThrowExceptionWhenPuzzleNotFound() {
            LocalDate today = LocalDate.now();
            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.getOrCreateTodaySession(userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Puzzle per la data " + today + " non trovato.");

            verify(puzzleService, times(1)).generateAndSavePuzzleForDate(today);
        }
    }

    @Nested
    @DisplayName("validateAndScoreWord Tests")
    class ValidateAndScoreWordTests {

        @Test
        @DisplayName("Should throw ResponseStatusException NOT_FOUND when session does not exist")
        void shouldThrowExceptionWhenSessionNotFound() {
            SubmitWordRequest request = new SubmitWordRequest("non-existent-session", "CASA");

            when(gameSessionRepository.findById(request.getSessionId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gameService.validateAndScoreWord(request, userId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Sessione di gioco non trovata");
        }

        @Test
        @DisplayName("Should throw ResponseStatusException FORBIDDEN when user is unauthorized")
        void shouldThrowExceptionWhenUserUnauthorized() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "CASA");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> gameService.validateAndScoreWord(request, "different-user-id"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Non sei autorizzato");
        }

        @Test
        @DisplayName("Should fail when word is too short (< 4 chars)")
        void shouldFailWhenWordIsTooShort() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "APA");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            SubmitWordResponse response = gameService.validateAndScoreWord(request, userId);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.TOO_SHORT);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }

        @Test
        @DisplayName("Should fail when word misses center letter")
        void shouldFailWhenWordMissesCenterLetter() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "EDILIZIA");

            session.getPuzzle().setCenterLetter("X");
            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            SubmitWordResponse response = gameService.validateAndScoreWord(request, userId);

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

            SubmitWordResponse response = gameService.validateAndScoreWord(request, userId);

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
            when(scoringService.calculateWordScore("CASA", false)).thenReturn(1);
            when(scoringService.calculateCurrentRank(1, 100)).thenReturn(RankTier.INITIAL);

            SubmitWordResponse response = gameService.validateAndScoreWord(request, userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getWord()).isEqualTo("CASA");
            assertThat(response.getPointsEarned()).isEqualTo(1);
            assertThat(response.getCurrentScore()).isEqualTo(1);
            assertThat(response.isMielegramma()).isFalse();

            verify(foundWordRepository, times(1)).save(any(FoundWord.class));
            verify(gameSessionRepository, atLeast(1)).save(session);
            verify(playerSeasonService, times(1)).updateSeasonProgress(userId, 1, false);
            verify(playerStatsService, times(1)).updatePlayerStatsAfterGame(userId, 1, "CASA", false);
            verify(badgeService, times(1)).evaluateAndAwardBadges(userId);
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
            when(scoringService.calculateWordScore("ALBERGO", true)).thenReturn(14);
            when(scoringService.calculateCurrentRank(14, 100)).thenReturn(RankTier.EXPERT);

            SubmitWordResponse response = gameService.validateAndScoreWord(request, userId);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getPointsEarned()).isEqualTo(14);
            assertThat(response.isMielegramma()).isTrue();

            verify(foundWordRepository, times(1)).save(any(FoundWord.class));
            verify(gameSessionRepository, atLeast(1)).save(session);
            verify(playerSeasonService, times(1)).updateSeasonProgress(userId, 14, false);
            verify(playerStatsService, times(1)).updatePlayerStatsAfterGame(userId, 14, "ALBERGO", false);
            verify(badgeService, times(1)).evaluateAndAwardBadges(userId);
        }

        @Test
        @DisplayName("Should fail with NOT_IN_PUZZLE when word is valid in dictionary but not in puzzle")
        void shouldFailWhenWordNotInPuzzleButInDictionary() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "ALBERO");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "ALBERO")).thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "ALBERO")).thenReturn(Optional.empty());
            when(dictionaryRepository.existsByWord("ALBERO")).thenReturn(true);

            SubmitWordResponse response = gameService.validateAndScoreWord(request, userId);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.NOT_IN_PUZZLE);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }

        @Test
        @DisplayName("Should fail with NOT_IN_DICTIONARY when word is not in dictionary")
        void shouldFailWhenWordNotInDictionary() {
            SubmitWordRequest request = new SubmitWordRequest(session.getId(), "ZZZA");

            when(gameSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "ZZZA")).thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "ZZZA")).thenReturn(Optional.empty());
            when(dictionaryRepository.existsByWord("ZZZA")).thenReturn(false);

            SubmitWordResponse response = gameService.validateAndScoreWord(request, userId);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorCode()).isEqualTo(ErrorTypeCode.NOT_IN_DICTIONARY);
            verify(invalidWordAttemptRepository, times(1)).save(any(InvalidWordAttempt.class));
        }
    }

    @Nested
    @DisplayName("syncLocalProgress Tests")
    class SyncLocalProgressTests {

        @Test
        @DisplayName("Should return empty list when request games list is null or empty")
        void shouldReturnEmptyWhenRequestIsEmpty() {
            GameBulkSyncRequest requestNull = new GameBulkSyncRequest(null);
            assertThat(gameService.syncLocalProgress(requestNull, userId)).isEmpty();

            GameBulkSyncRequest requestEmpty = new GameBulkSyncRequest(Collections.emptyList());
            assertThat(gameService.syncLocalProgress(requestEmpty, userId)).isEmpty();
        }

        @Test
        @DisplayName("Should sync local progress in chronological order and update session score and global stats")
        void shouldSyncLocalProgressInChronologicalOrder() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            GameSyncRequest syncYesterday = new GameSyncRequest();
            syncYesterday.setPuzzleDate(yesterday);
            syncYesterday.setFoundWords(List.of("CASA"));

            GameSyncRequest syncToday = new GameSyncRequest();
            syncToday.setPuzzleDate(today);
            syncToday.setFoundWords(List.of("ALBERO"));

            GameBulkSyncRequest request = new GameBulkSyncRequest(List.of(syncToday, syncYesterday));

            DailyPuzzle yesterdayPuzzle = DailyPuzzle.builder()
                    .id("p-yesterday")
                    .puzzleDate(yesterday)
                    .centerLetter("A")
                    .outerLetters(buildOuterLetters("p-yesterday", List.of("B", "C", "E", "F", "M", "O")))
                    .maxScore(100)
                    .build();

            DailyPuzzle todayPuzzle = DailyPuzzle.builder()
                    .id("p-today")
                    .puzzleDate(today)
                    .centerLetter("A")
                    .outerLetters(buildOuterLetters("p-today", List.of("B", "C", "E", "F", "M", "O")))
                    .maxScore(100)
                    .build();

            GameSession yesterdaySession = GameSession.builder().id("s-yesterday").user(user).puzzle(yesterdayPuzzle)
                    .currentScore(0).build();
            GameSession todaySession = GameSession.builder().id("s-today").user(user).puzzle(todayPuzzle)
                    .currentScore(0).build();

            when(dailyPuzzleRepository.findByPuzzleDate(yesterday)).thenReturn(Optional.of(yesterdayPuzzle));
            when(dailyPuzzleRepository.findByPuzzleDate(today)).thenReturn(Optional.of(todayPuzzle));

            when(gameSessionRepository.findByUserIdAndPuzzleId(userId, yesterdayPuzzle.getId()))
                    .thenReturn(Optional.of(yesterdaySession));
            when(gameSessionRepository.findByUserIdAndPuzzleId(userId, todayPuzzle.getId()))
                    .thenReturn(Optional.of(todaySession));

            when(foundWordRepository.existsByIdSessionIdAndIdWord("s-yesterday", "CASA")).thenReturn(false);
            when(foundWordRepository.existsByIdSessionIdAndIdWord("s-today", "ALBERO")).thenReturn(false);

            PuzzleWord pwCasa = PuzzleWord.builder().isMielegramma(false).build();
            PuzzleWord pwAlbero = PuzzleWord.builder().isMielegramma(true).build();

            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(yesterdayPuzzle.getId(), "CASA"))
                    .thenReturn(Optional.of(pwCasa));
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(todayPuzzle.getId(), "ALBERO"))
                    .thenReturn(Optional.of(pwAlbero));

            when(scoringService.calculateWordScore("CASA", false)).thenReturn(1);
            when(scoringService.calculateWordScore("ALBERO", true)).thenReturn(14);

            when(scoringService.calculateCurrentRank(1, 100)).thenReturn(RankTier.INITIAL);
            when(scoringService.calculateCurrentRank(14, 100)).thenReturn(RankTier.EXPERT);

            FoundWord fw1 = FoundWord.builder().id(new FoundWordId("s-yesterday", "CASA")).build();
            FoundWord fw2 = FoundWord.builder().id(new FoundWordId("s-today", "ALBERO")).build();
            when(foundWordRepository.findByIdSessionId("s-yesterday")).thenReturn(List.of(fw1));
            when(foundWordRepository.findByIdSessionId("s-today")).thenReturn(List.of(fw2));

            List<GameSessionResponse> responses = gameService.syncLocalProgress(request, userId);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getId()).isEqualTo("s-yesterday");
            assertThat(responses.get(1).getId()).isEqualTo("s-today");

            verify(puzzleService, times(1)).generateAndSavePuzzleForDate(yesterday);
            verify(puzzleService, times(1)).generateAndSavePuzzleForDate(today);

            verify(foundWordRepository, times(2)).save(any(FoundWord.class));
            verify(gameSessionRepository, times(1)).save(yesterdaySession);
            verify(gameSessionRepository, times(1)).save(todaySession);

            verify(playerSeasonService, times(1)).updateSeasonProgress(userId, 15, false);
            verify(playerStatsService, times(1)).updatePlayerStatsAfterGame(userId, 15, "ALBERO", false);
            verify(badgeService, times(1)).evaluateAndAwardBadges(userId);
        }

        @Test
        @DisplayName("Should process found words and invalid word attempts in bulk sync")
        void shouldSyncFoundWordsAndInvalidWordsCorrectly() {
            LocalDate puzzleDate = LocalDate.now();
            GameSyncRequest syncRequest = new GameSyncRequest();
            syncRequest.setPuzzleDate(puzzleDate);
            syncRequest.setFoundWords(List.of("CASA"));

            String invalidWord = "AMBEFA";
            syncRequest.setInvalidWords(List.of(invalidWord));

            GameBulkSyncRequest request = new GameBulkSyncRequest(List.of(syncRequest));
            PuzzleWord pw = PuzzleWord.builder().isMielegramma(false).build();

            when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));
            when(gameSessionRepository.findByUserIdAndPuzzleId(userId, puzzle.getId()))
                    .thenReturn(Optional.of(session));

            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "CASA")).thenReturn(false);
            when(puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), "CASA")).thenReturn(Optional.of(pw));
            when(scoringService.calculateWordScore("CASA", false)).thenReturn(1);
            when(scoringService.calculateCurrentRank(1, 100)).thenReturn(RankTier.INITIAL);

            when(invalidWordAttemptRepository.findDistinctAttemptedWordsBySessionId(session.getId()))
                    .thenReturn(Collections.emptyList());

            FoundWord fw = FoundWord.builder().id(new FoundWordId(session.getId(), "CASA")).build();
            when(foundWordRepository.findByIdSessionId(session.getId())).thenReturn(List.of(fw));

            List<GameSessionResponse> responses = gameService.syncLocalProgress(request, userId);

            assertThat(responses).hasSize(1);
            verify(foundWordRepository, times(1)).save(foundWordCaptor.capture());
            verify(invalidWordAttemptRepository, times(1)).save(invalidAttemptCaptor.capture());

            assertThat(foundWordCaptor.getValue().getScoreAssigned()).isEqualTo(1);
            assertThat(invalidAttemptCaptor.getValue().getAttemptedWord()).isEqualTo(invalidWord);

            verify(playerSeasonService, times(1)).updateSeasonProgress(userId, 1, false);
            verify(playerStatsService, times(1)).updatePlayerStatsAfterGame(userId, 1, "CASA", false);
            verify(badgeService, times(1)).evaluateAndAwardBadges(userId);
        }

        @Test
        @DisplayName("Should not execute global side effects if no new points earned and no completion in bulk")
        void shouldNotTriggerGlobalSideEffectsWhenNoProgressMade() {
            LocalDate puzzleDate = LocalDate.now();
            GameSyncRequest syncRequest = new GameSyncRequest();
            syncRequest.setPuzzleDate(puzzleDate);
            syncRequest.setFoundWords(List.of("CASA"));

            GameBulkSyncRequest request = new GameBulkSyncRequest(List.of(syncRequest));

            when(dailyPuzzleRepository.findByPuzzleDate(puzzleDate)).thenReturn(Optional.of(puzzle));
            when(gameSessionRepository.findByUserIdAndPuzzleId(userId, puzzle.getId()))
                    .thenReturn(Optional.of(session));
            when(foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), "CASA")).thenReturn(true);
            when(foundWordRepository.findByIdSessionId(session.getId())).thenReturn(Collections.emptyList());

            List<GameSessionResponse> responses = gameService.syncLocalProgress(request, userId);

            assertThat(responses).hasSize(1);
            verify(playerSeasonService, never()).updateSeasonProgress(anyString(), anyInt(), anyBoolean());
            verify(playerStatsService, never()).updatePlayerStatsAfterGame(anyString(), anyInt(), anyString(),
                    anyBoolean());
            verify(badgeService, never()).evaluateAndAwardBadges(anyString());
        }
    }
}