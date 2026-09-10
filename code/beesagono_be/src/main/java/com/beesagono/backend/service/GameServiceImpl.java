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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
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
    private final PuzzleGeneratorService puzzleService;

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
        GameSession session = gameSessionRepository.findById(request.getSessionId())
                .orElseThrow(
                        () -> new RuntimeException("Sessione di gioco non trovata con ID: " + request.getSessionId()));

        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Non sei autorizzato a modificare questa sessione di gioco.");
        }

        String rawWord = request.getWord();

        // Minimum Length Validation
        if (rawWord == null || rawWord.isBlank() || rawWord.trim().length() < 4) {
            recordInvalidAttempt(session, rawWord, ErrorTypeCode.TOO_SHORT);
            return buildErrorResponse(rawWord, session, ErrorTypeCode.TOO_SHORT,
                    "La parola deve contenere almeno 4 lettere.");
        }

        String word = rawWord.trim().toUpperCase();

        // Center Letter Validation
        if (!word.contains(session.getPuzzle().getCenterLetter())) {
            recordInvalidAttempt(session, word, ErrorTypeCode.MISSING_CENTER);
            return buildErrorResponse(word, session, ErrorTypeCode.MISSING_CENTER,
                    "La parola non contiene la lettera centrale obbligatoria.");
        }

        // Duplicate Check in Session
        if (foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), word)) {
            recordInvalidAttempt(session, word, ErrorTypeCode.ALREADY_FOUND);
            return buildErrorResponse(word, session, ErrorTypeCode.ALREADY_FOUND, "Hai già trovato questa parola!");
        }

        // Verification in Today's Puzzle
        Optional<PuzzleWord> puzzleWord = puzzleWordRepository.findByIdPuzzleIdAndIdWord(session.getPuzzle().getId(),
                word);

        if (puzzleWord.isPresent()) {
            PuzzleWord pw = puzzleWord.get();
            boolean isMiele = Boolean.TRUE.equals(pw.getIsMielegramma());
            int basePoints = word.length() == 4 ? 1 : word.length();
            int pointsEarned = isMiele ? basePoints + 7 : basePoints;

            FoundWord foundWord = FoundWord.builder()
                    .id(new FoundWordId(session.getId(), word))
                    .session(session)
                    .scoreAssigned(pointsEarned)
                    .isMielegramma(isMiele)
                    .build();
            foundWordRepository.save(foundWord);

            int updatedScore = session.getCurrentScore() + pointsEarned;
            session.setCurrentScore(updatedScore);

            updateSessionRankAndCompletion(session, updatedScore);
            gameSessionRepository.save(session);

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

        // If not in puzzle, check in Global Dictionary
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

        for (GameSyncRequest singleSync : request.getGames()) {
            if (singleSync.getPuzzleDate() == null) {
                continue;
            }

            // Initialize puzzle for date if it doesn't exist yet
            puzzleService.generateAndSavePuzzleForDate(singleSync.getPuzzleDate());

            DailyPuzzle puzzle = dailyPuzzleRepository.findByPuzzleDate(singleSync.getPuzzleDate())
                    .orElseThrow(() -> new RuntimeException(
                            "Errore nel recupero del puzzle per la data: " + singleSync.getPuzzleDate()));

            // Check Inconsistencies: If center letter doesn't match, ignore FE
            if (StringUtils.hasText(singleSync.getCenterLetter()) &&
                    !singleSync.getCenterLetter().trim().equalsIgnoreCase(puzzle.getCenterLetter())) {
                continue;
            }

            // Retrieve or create user session in DB
            GameSession session = gameSessionRepository.findByUserIdAndPuzzleId(userId, puzzle.getId())
                    .orElseGet(() -> createNewSession(userId, puzzle));

            // Process and validate words submitted by FE
            if (singleSync.getFoundWords() != null && !singleSync.getFoundWords().isEmpty()) {
                for (String rawWord : singleSync.getFoundWords()) {
                    if (rawWord == null || rawWord.isBlank()) {
                        continue;
                    }
                    String word = rawWord.trim().toUpperCase();

                    if (!foundWordRepository.existsByIdSessionIdAndIdWord(session.getId(), word)) {
                        puzzleWordRepository.findByIdPuzzleIdAndIdWord(puzzle.getId(), word).ifPresent(pw -> {
                            boolean isMiele = Boolean.TRUE.equals(pw.getIsMielegramma());
                            int basePoints = word.length() == 4 ? 1 : word.length();
                            int pointsEarned = isMiele ? basePoints + 7 : basePoints;

                            FoundWord foundWord = FoundWord.builder()
                                    .id(new FoundWordId(session.getId(), word))
                                    .session(session)
                                    .scoreAssigned(pointsEarned)
                                    .isMielegramma(isMiele)
                                    .build();
                            foundWordRepository.save(foundWord);

                            session.setCurrentScore(session.getCurrentScore() + pointsEarned);
                        });
                    }
                }

                updateSessionRankAndCompletion(session, session.getCurrentScore());
                gameSessionRepository.save(session);
            }

            responses.add(buildGameSessionResponse(session));
        }

        return responses;
    }

    // --- Private Helper Methods ---

    private GameSession createNewSession(String userId, DailyPuzzle puzzle) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));

        Date now = new Date();

        GameSession newSession = GameSession.builder()
                .puzzle(puzzle)
                .user(user)
                .currentScore(0)
                .currentRankLabel(RankTier.BEGINNER.getLabel())
                .isCompleted(false)
                .startTime(now)
                .lastUpdated(now)
                .build();

        return gameSessionRepository.save(newSession);
    }

    private void updateSessionRankAndCompletion(GameSession session, int score) {
        double percentage = ((double) score / session.getPuzzle().getMaxScore()) * 100.0;
        RankTier rank = RankTier.getRankForPercentage(percentage);
        session.setCurrentRankLabel(rank.getLabel());

        if (percentage >= 100.0) {
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
        List<FoundWord> foundWords = foundWordRepository.findByIdSessionId(session.getId());
        Set<String> wordsSet = foundWords.stream()
                .map(fw -> fw.getId().getWord())
                .collect(Collectors.toSet());

        return GameSessionResponse.builder()
                .id(session.getId())
                .puzzleId(session.getPuzzle().getId())
                .userId(session.getUser().getId())
                .currentScore(session.getCurrentScore())
                .currentRankLabel(session.getCurrentRankLabel())
                .isCompleted(session.getIsCompleted())
                .foundWords(wordsSet)
                .startTime(session.getStartTime())
                .lastUpdated(session.getLastUpdated())
                .build();
    }
}