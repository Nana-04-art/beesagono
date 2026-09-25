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
import com.beesagono.backend.entity.RankHistogram;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.entity.id.FoundWordId;
import com.beesagono.backend.entity.id.RankHistogramId;
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

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final GameSessionRepository gameSessionRepository;
    private final FoundWordRepository foundWordRepository;
    private final DailyPuzzleRepository dailyPuzzleRepository;
    private final PuzzleWordRepository puzzleWordRepository;
    private final InvalidWordAttemptRepository invalidWordAttemptRepository;
    private final UserRepository userRepository;
    private final DictionaryWordRepository dictionaryRepository;
    private final RankHistogramRepository rankHistogramRepository;
    private final PlayerStatsService playerStatsService;
    private final PuzzleGeneratorService puzzleService;
    private final ScoringService scoringService;
    private final PlayerSeasonService playerSeasonService;
    private final BadgeService badgeService;

    @Override
    @Transactional
    public GameSessionResponse getOrCreateTodaySession(String userId) {
        LocalDate today = LocalDate.now();

        // Generate and save today's puzzle if it doesn't exist yet
        puzzleService.generateAndSavePuzzleForDate(today);

        DailyPuzzle todayPuzzle = dailyPuzzleRepository.findByPuzzleDate(today)
                .orElseThrow(() -> new RuntimeException("Puzzle per la data " + today + " non trovato."));

        GameSession session = gameSessionRepository.findByUserIdAndPuzzleId(userId, todayPuzzle.getId())
                .orElseGet(() -> createNewSession(userId, todayPuzzle));

        return buildGameSessionResponse(session);
    }

    @Override
    @Transactional
    public SubmitWordResponse validateAndScoreWord(SubmitWordRequest request, String userId) {
        // Session and Puzzle existence check
        GameSession session = gameSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Sessione di gioco non trovata con ID: " + request.getSessionId()));

        if (session.getPuzzle() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Puzzle non trovato per la sessione di gioco indicata.");
        }

        // User Authorization Check
        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Non sei autorizzato a modificare questa sessione di gioco.");
        }

        // Every word submission attempt (valid or invalid) records today's play for the
        // streak
        session.setLastUpdated(new Date());
        gameSessionRepository.save(session);

        String rawWord = request.getWord();

        // Syntactic input validation (Minimum length)
        if (rawWord == null || rawWord.isBlank() || rawWord.trim().length() < 4) {
            recordInvalidAttempt(session, rawWord, ErrorTypeCode.TOO_SHORT);
            return buildErrorResponse(rawWord, session, ErrorTypeCode.TOO_SHORT,
                    "La parola deve contenere almeno 4 lettere.");
        }

        String word = rawWord.trim().toUpperCase();

        // Duplicate word check in session
        if (foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), word)) {
            recordInvalidAttempt(session, word, ErrorTypeCode.ALREADY_FOUND);
            return buildErrorResponse(word, session, ErrorTypeCode.ALREADY_FOUND, "Hai già trovato questa parola!");
        }

        // Mandatory center letter check
        if (!word.contains(session.getPuzzle().getCenterLetter())) {
            recordInvalidAttempt(session, word, ErrorTypeCode.MISSING_CENTER);
            return buildErrorResponse(word, session, ErrorTypeCode.MISSING_CENTER,
                    "La parola non contiene la lettera centrale obbligatoria.");
        }

        // Solution verification in daily puzzle and score calculation
        Optional<PuzzleWord> puzzleWord = puzzleWordRepository.findByIdPuzzleIdAndIdWord(session.getPuzzle().getId(),
                word);

        if (puzzleWord.isPresent()) {
            PuzzleWord pw = puzzleWord.get();
            boolean isMiele = Boolean.TRUE.equals(pw.getIsMielegramma());

            int pointsEarned = scoringService.calculateWordScore(word, isMiele);

            FoundWord foundWord = FoundWord.builder()
                    .id(new FoundWordId(session.getId(), word))
                    .session(session)
                    .scoreAssigned(pointsEarned)
                    .isMielegramma(isMiele)
                    .build();
            foundWordRepository.save(foundWord);

            int updatedScore = session.getCurrentScore() + pointsEarned;
            session.setCurrentScore(updatedScore);

            boolean isCompletedNow = !Boolean.TRUE.equals(session.getIsCompleted())
                    && updatedScore >= session.getPuzzle().getMaxScore();

            updateSessionRankAndCompletion(session, updatedScore);
            gameSessionRepository.save(session);

            // Atomic season update (annual career, streak, points, and completion)
            playerSeasonService.updateSeasonProgress(userId, pointsEarned, isCompletedNow);
            playerStatsService.updatePlayerStatsAfterGame(userId, pointsEarned, word, isCompletedNow);

            // Automatic badge evaluation and awarding
            badgeService.evaluateAndAwardBadges(userId);

            return SubmitWordResponse.builder()
                    .success(true)
                    .word(word)
                    .pointsEarned(pointsEarned)
                    .currentScore(updatedScore)
                    .currentRankLabel(session.getCurrentRankLabel())
                    .isMielegramma(isMiele)
                    .errorCode(null)
                    .errorMessage(null)
                    .build();
        }

        // Global Dictionary check (Distinguishes between missing from puzzle vs missing
        // from dictionary)
        boolean existsInDictionary = dictionaryRepository.existsByWord(word);

        if (existsInDictionary) {
            recordInvalidAttempt(session, word, ErrorTypeCode.NOT_IN_PUZZLE);
            return buildErrorResponse(word, session, ErrorTypeCode.NOT_IN_PUZZLE,
                    "La parola non fa parte delle soluzioni del puzzle di oggi.");
        } else {
            recordInvalidAttempt(session, word, ErrorTypeCode.NOT_IN_DICTIONARY);
            return buildErrorResponse(word, session, ErrorTypeCode.NOT_IN_DICTIONARY,
                    "La parola non è presente nel dizionario ufficiale.");
        }
    }

    @Override
    @Transactional
    public List<GameSessionResponse> syncLocalProgress(GameBulkSyncRequest request, String userId) {
        List<GameSessionResponse> responses = new ArrayList<>();
        if (request.getGames() == null || request.getGames().isEmpty()) {
            return responses;
        }

        // ASCENDING CHRONOLOGICAL ORDER (CRUCIAL FOR STREAK TRACKING)
        // Sort sessions from oldest to newest before processing
        List<GameSyncRequest> sortedGames = request.getGames().stream()
                .filter(g -> g.getPuzzleDate() != null)
                .sorted(Comparator.comparing(GameSyncRequest::getPuzzleDate))
                .toList();

        // Accumulators for global side-effects (executed once at the end of the batch)
        int totalNewPointsInBulk = 0;
        String longestWordInBulk = null;
        boolean anyCompletedInBulk = false;

        for (GameSyncRequest singleSync : sortedGames) {

            // Retrieve or generate the official BE puzzle for that date
            puzzleService.generateAndSavePuzzleForDate(singleSync.getPuzzleDate());
            DailyPuzzle puzzle = dailyPuzzleRepository.findByPuzzleDate(singleSync.getPuzzleDate())
                    .orElseThrow(() -> new RuntimeException(
                            "Errore nel recupero del puzzle per la data: " + singleSync.getPuzzleDate()));

            // Retrieve or create the game session in DB
            GameSession session = gameSessionRepository.findByUserIdAndPuzzleId(userId, puzzle.getId())
                    .orElseGet(() -> createNewSession(userId, puzzle));

            int newPointsEarnedInSession = 0;

            // CASE 1: Multi-Device Sync with compatible dictionary (Merge unique words)
            // Process and merge VALID found words
            if (singleSync.getFoundWords() != null && !singleSync.getFoundWords().isEmpty()) {
                for (String rawWord : singleSync.getFoundWords()) {
                    if (!StringUtils.hasText(rawWord)) {
                        continue;
                    }
                    String word = rawWord.trim().toUpperCase();

                    // If the word has not been saved in this session yet
                    if (!foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), word)) {
                        Optional<PuzzleWord> pwOpt = puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(),
                                word);

                        // Verify that the word officially exists in the DB for this puzzle
                        if (pwOpt.isPresent()) {
                            PuzzleWord pw = pwOpt.get();
                            boolean isMiele = Boolean.TRUE.equals(pw.getIsMielegramma());
                            int pointsEarned = scoringService.calculateWordScore(word, isMiele);

                            newPointsEarnedInSession += pointsEarned;

                            // Track the longest word found across the entire batch
                            if (longestWordInBulk == null || word.length() > longestWordInBulk.length()) {
                                longestWordInBulk = word;
                            }

                            FoundWord foundWord = FoundWord.builder()
                                    .id(new FoundWordId(session.getId(), word))
                                    .session(session)
                                    .scoreAssigned(pointsEarned)
                                    .isMielegramma(isMiele)
                                    .build();
                            foundWordRepository.save(foundWord);
                        }
                    }
                }
            }

            // Process and save INVALID word attempts
            if (singleSync.getInvalidWords() != null && !singleSync.getInvalidWords().isEmpty()) {
                List<String> existingInvalidWords = invalidWordAttemptRepository
                        .findDistinctAttemptedWordsBySessionId(session.getId());

                Set<String> existingSet = new HashSet<>(existingInvalidWords);

                for (String rawInvalidWord : singleSync.getInvalidWords()) {
                    if (!StringUtils.hasText(rawInvalidWord)) {
                        continue;
                    }
                    String invalidWord = rawInvalidWord.trim().toUpperCase();

                    if (!existingSet.contains(invalidWord)) {
                        ErrorTypeCode reason = determineErrorReason(invalidWord, puzzle);

                        InvalidWordAttempt attempt = InvalidWordAttempt.builder()
                                .session(session)
                                .attemptedWord(invalidWord)
                                .errorReason(reason)
                                .build();

                        invalidWordAttemptRepository.save(attempt);
                        existingSet.add(invalidWord);
                    }
                }
            }

            // Update current session if new progress was made
            if (newPointsEarnedInSession > 0) {
                int updatedSessionScore = session.getCurrentScore() + newPointsEarnedInSession;
                session.setCurrentScore(updatedSessionScore);

                boolean isCompletedNow = !Boolean.TRUE.equals(session.getIsCompleted())
                        && updatedSessionScore >= puzzle.getMaxScore();

                updateSessionRankAndCompletion(session, updatedSessionScore);
                gameSessionRepository.save(session);

                totalNewPointsInBulk += newPointsEarnedInSession;
                if (isCompletedNow) {
                    anyCompletedInBulk = true;
                }
            }

            responses.add(buildGameSessionResponse(session));
        }

        // Global side effects executed ONLY ONCE at the end of the Bulk Sync
        if (totalNewPointsInBulk > 0 || anyCompletedInBulk) {
            playerSeasonService.updateSeasonProgress(userId, totalNewPointsInBulk, anyCompletedInBulk);
            playerStatsService.updatePlayerStatsAfterGame(userId, totalNewPointsInBulk, longestWordInBulk,
                    anyCompletedInBulk);

            // Uniform badge calculation for the bulk request
            badgeService.evaluateAndAwardBadges(userId);
        }

        return responses;
    }

    // --- Private Helper Methods ---

    private GameSession createNewSession(String userId, DailyPuzzle puzzle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));

        // Record that the user started today's match
        playerSeasonService.updateSeasonProgress(userId, 0, false);

        Date now = new Date();

        GameSession newSession = GameSession.builder()
                .puzzle(puzzle)
                .user(user)
                .currentScore(0)
                .currentRankLabel(RankTier.INITIAL.getLabel())
                .isCompleted(false)
                .startTime(now)
                .lastUpdated(now)
                .build();

        return gameSessionRepository.save(newSession);
    }

    private void updateSessionRankAndCompletion(GameSession session, int newScore) {
        RankTier newRank = scoringService.calculateCurrentRank(newScore, session.getPuzzle().getMaxScore());

        // If the user has moved up a rank (label different from the current one),
        // increment the RankHistogram
        if (!newRank.getLabel().equals(session.getCurrentRankLabel())) {
            session.setCurrentRankLabel(newRank.getLabel());

            RankHistogramId rankId = new RankHistogramId(session.getUser().getId(), newRank.name());
            RankHistogram rankHistogram = rankHistogramRepository.findById(rankId)
                    .orElseGet(() -> RankHistogram.builder()
                            .id(rankId)
                            .user(session.getUser())
                            .count(0)
                            .build());

            rankHistogram.setCount(rankHistogram.getCount() + 1);
            rankHistogramRepository.save(rankHistogram);
        }

        if (newScore >= session.getPuzzle().getMaxScore()) {
            session.setIsCompleted(true);
        }
    }

    private void recordInvalidAttempt(GameSession session, String word, ErrorTypeCode reason) {
        InvalidWordAttempt attempt = InvalidWordAttempt.builder()
                .session(session)
                .attemptedWord(word != null ? word : "")
                .errorReason(reason)
                .build();
        invalidWordAttemptRepository.save(attempt);

        // Notify today's activity to PlayerSeasonService (0 points earned, but played
        // day)
        playerSeasonService.updateSeasonProgress(session.getUser().getId(), 0, false);
    }

    private SubmitWordResponse buildErrorResponse(String word, GameSession session, ErrorTypeCode code,
            String message) {
        return SubmitWordResponse.builder()
                .success(false)
                .word(word)
                .pointsEarned(0)
                .currentScore(session.getCurrentScore())
                .currentRankLabel(session.getCurrentRankLabel())
                .isMielegramma(false)
                .errorCode(code)
                .errorMessage(message)
                .build();
    }

    private GameSessionResponse buildGameSessionResponse(GameSession session) {
        // Retrieve valid found words
        List<FoundWord> foundWordsEntities = foundWordRepository.findByIdSessionId(session.getId());

        Set<String> foundWords = foundWordsEntities.stream()
                .map(fw -> fw.getId().getWord())
                .collect(Collectors.toSet());

        // Retrieve found Mielegrammi
        Set<String> foundMielegrammi = foundWordsEntities.stream()
                .filter(fw -> Boolean.TRUE.equals(fw.getIsMielegramma()))
                .map(fw -> fw.getId().getWord())
                .collect(Collectors.toSet());

        // Retrieve session's invalid word attempts
        List<String> invalidWordsList = invalidWordAttemptRepository
                .findDistinctAttemptedWordsBySessionId(session.getId());
        Set<String> invalidWords = invalidWordsList != null ? Set.copyOf(invalidWordsList) : Set.of();

        return GameSessionResponse.builder()
                .id(session.getId())
                .puzzleId(session.getPuzzle() != null ? session.getPuzzle().getId() : null)
                .userId(session.getUser() != null ? session.getUser().getId() : null)
                .currentScore(session.getCurrentScore())
                .currentRankLabel(session.getCurrentRankLabel())
                .isCompleted(session.getIsCompleted())
                .foundWords(foundWords)
                .invalidWords(invalidWords)
                .foundMielegrammi(foundMielegrammi)
                .startTime(session.getStartTime())
                .lastUpdated(session.getLastUpdated())
                .build();
    }

    private ErrorTypeCode determineErrorReason(String word, DailyPuzzle puzzle) {
        if (word == null || word.isBlank()) {
            return ErrorTypeCode.NOT_IN_DICTIONARY;
        }

        String normalizedWord = word.trim().toUpperCase();

        // Check minimum length (e.g., 4 characters)
        if (normalizedWord.length() < 4) {
            return ErrorTypeCode.TOO_SHORT;
        }

        // Check presence of the mandatory center letter
        String center = puzzle.getCenterLetter().toUpperCase();
        if (!normalizedWord.contains(center)) {
            return ErrorTypeCode.MISSING_CENTER;
        }

        // Extract all valid letters (Center + Outer Letters from EmbeddedId)
        Set<Character> allowedChars = new HashSet<>();
        allowedChars.add(center.charAt(0));

        if (puzzle.getOuterLetters() != null) {
            for (PuzzleOuterLetter outer : puzzle.getOuterLetters()) {
                if (outer.getId() != null && outer.getId().getLetter() != null) {
                    String letter = outer.getId().getLetter().trim().toUpperCase();
                    if (!letter.isEmpty()) {
                        allowedChars.add(letter.charAt(0));
                    }
                }
            }
        }

        // Verify that the word contains only allowed characters
        for (char c : normalizedWord.toCharArray()) {
            if (!allowedChars.contains(c)) {
                return ErrorTypeCode.INVALID_LETTERS;
            }
        }

        // If it satisfies all composition rules but is not in the dictionary
        return ErrorTypeCode.NOT_IN_DICTIONARY;
    }
}