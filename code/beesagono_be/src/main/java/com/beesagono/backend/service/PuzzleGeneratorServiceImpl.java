package com.beesagono.backend.service;

import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import com.beesagono.backend.entity.PuzzleWord;
import com.beesagono.backend.entity.id.PuzzleOuterLetterId;
import com.beesagono.backend.entity.id.PuzzleWordId;
import com.beesagono.backend.repository.DailyPuzzleRepository;
import com.beesagono.backend.repository.DictionaryWordRepository;
import com.beesagono.backend.repository.PuzzleOuterLetterRepository;
import com.beesagono.backend.repository.PuzzleWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PuzzleGeneratorServiceImpl implements PuzzleGeneratorService {

    private final DictionaryWordRepository dictionaryWordRepository;
    private final DailyPuzzleRepository dailyPuzzleRepository;
    private final PuzzleOuterLetterRepository puzzleOuterLetterRepository;
    private final PuzzleWordRepository puzzleWordRepository;

    private static final int MIN_WORD_LENGTH = 4;
    private static final int REQUIRED_LETTERS_COUNT = 7;
    private static final int MIN_TARGET_WORDS_COUNT = 20;
    private static final int MIN_MIELEGRAMMI_COUNT = 1;
    private static final int MAX_GENERATION_ATTEMPTS = 50;
    private static final int MIELEGRAMMA_BONUS = 7;
    private static final int RECENT_CENTER_LETTERS_LIMIT = 3;

    @Override
    @Transactional
    public void generateAndSavePuzzleForDate(LocalDate date) {
        String dateStr = date.toString();
        if (dailyPuzzleRepository.existsByPuzzleDate(date)) {
            log.info("Puzzle per la data {} già esistente a database.", dateStr);
            return;
        }

        // Retrieve the middle letters of the last 3 puzzles to avoid repetitions
        List<String> recentCenterLetters = dailyPuzzleRepository
                .findAllByOrderByPuzzleDateDesc(PageRequest.of(0, RECENT_CENTER_LETTERS_LIMIT))
                .stream()
                .map(DailyPuzzle::getCenterLetter)
                .toList();

        // Extraction of pangram candidates
        List<String> pangramCandidates = dictionaryWordRepository.findCandidatePangrams();
        List<String> safeCandidates = pangramCandidates.isEmpty()
                ? List.of("ALBERGO")
                : pangramCandidates;

        long baseSeed = hashDateString(dateStr);
        Supplier<Double> rng = mulberry32(baseSeed);

        GeneratedBoard selectedBoard = null;
        GeneratedBoard fallbackBoard = null;

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String targetPangram = safeCandidates.get((int) Math.floor(rng.get() * safeCandidates.size()));

            List<String> uniqueLetters = targetPangram.chars()
                    .mapToObj(c -> String.valueOf((char) c))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            // Filter by excluding recent central letters.
            List<String> preferredLetters = uniqueLetters.stream()
                    .filter(l -> !recentCenterLetters.contains(l))
                    .collect(Collectors.toList());

            // If all the letters of the pangram have been used recently, use the full set.
            List<String> candidateLetters = preferredLetters.isEmpty() ? uniqueLetters : preferredLetters;

            String centerLetter = candidateLetters.get((int) Math.floor(rng.get() * candidateLetters.size()));

            // Calculation of Bitmasks
            int puzzleMask = calculateMaskFromString(targetPangram);
            int centerBit = 1 << (centerLetter.charAt(0) - 'A');

            // JPQL bitwise query to retrieve valid words
            List<DictionaryWord> validWordsFromDb = dictionaryWordRepository.findValidWordsForPuzzle(centerBit,
                    puzzleMask);

            List<PuzzleWordData> targetWords = new ArrayList<>();
            List<String> mielegrammi = new ArrayList<>();

            for (DictionaryWord dw : validWordsFromDb) {
                String word = dw.getWord().toUpperCase();
                boolean isMielegramma = dw.getUniqueLettersCount() == REQUIRED_LETTERS_COUNT;

                if (isMielegramma) {
                    mielegrammi.add(word);
                }

                int basePoints = word.length() == MIN_WORD_LENGTH ? 1 : word.length();
                int bonus = isMielegramma ? MIELEGRAMMA_BONUS : 0;

                targetWords.add(new PuzzleWordData(dw, isMielegramma, basePoints + bonus));
            }

            GeneratedBoard currentBoard = new GeneratedBoard(
                    centerLetter,
                    uniqueLetters,
                    targetWords,
                    baseSeed + "_" + attempt);

            // Quality Gate check
            if (targetWords.size() >= MIN_TARGET_WORDS_COUNT && mielegrammi.size() >= MIN_MIELEGRAMMI_COUNT) {
                selectedBoard = currentBoard;
                break;
            }

            if (attempt == MAX_GENERATION_ATTEMPTS - 1) {
                fallbackBoard = currentBoard;
            }
        }

        GeneratedBoard boardToSave = selectedBoard != null ? selectedBoard : fallbackBoard;

        if (boardToSave != null) {
            int maxScore = boardToSave.words().stream().mapToInt(PuzzleWordData::score).sum();

            DailyPuzzle dailyPuzzle = DailyPuzzle.builder()
                    .puzzleDate(date)
                    .centerLetter(boardToSave.centerLetter())
                    .maxScore(maxScore)
                    .seed(boardToSave.seed())
                    .build();

            DailyPuzzle savedPuzzle = dailyPuzzleRepository.save(dailyPuzzle);

            // Batch saving of outer letters
            List<PuzzleOuterLetter> outerLetters = boardToSave.uniqueLetters().stream()
                    .filter(l -> !l.equals(boardToSave.centerLetter()))
                    .map(letter -> PuzzleOuterLetter.builder()
                            .id(new PuzzleOuterLetterId(savedPuzzle.getId(), letter))
                            .puzzle(savedPuzzle)
                            .build())
                    .collect(Collectors.toList());

            puzzleOuterLetterRepository.saveAll(outerLetters);

            // Batch saving of valid words in the puzzle
            List<PuzzleWord> puzzleWords = boardToSave.words().stream()
                    .map(pwd -> PuzzleWord.builder()
                            .id(new PuzzleWordId(savedPuzzle.getId(), pwd.dictEntity().getWord()))
                            .puzzle(savedPuzzle)
                            .dictionaryWord(pwd.dictEntity())
                            .isMielegramma(pwd.isPangram())
                            .build())
                    .collect(Collectors.toList());

            puzzleWordRepository.saveAll(puzzleWords);

            log.info(">>> DailyPuzzle per il {} generato con successo! (Centro: {}, Seed: {}, MaxScore: {})",
                    date, boardToSave.centerLetter(), boardToSave.seed(), maxScore);
        }
    }

    // --- Utility Bitmask and PRNG Algorithms ---

    private int calculateMaskFromString(String str) {
        int mask = 0;
        for (char c : str.toUpperCase().toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                mask |= (1 << (c - 'A'));
            }
        }
        return mask;
    }

    private long hashDateString(String dateStr) {
        long hash = 5381L;
        for (int i = 0; i < dateStr.length(); i++) {
            hash = ((hash * 33) ^ dateStr.charAt(i)) & 0xFFFFFFFFL;
        }
        return hash;
    }

    private Supplier<Double> mulberry32(long seed) {
        long[] state = new long[] { seed & 0xFFFFFFFFL };
        return () -> {
            state[0] = (state[0] + 0x6D2B79F5L) & 0xFFFFFFFFL;
            long t = state[0];

            long term1 = (t ^ (t >>> 15)) & 0xFFFFFFFFL;
            long term2 = (t | 1L) & 0xFFFFFFFFL;
            t = (term1 * term2) & 0xFFFFFFFFL;

            long term3 = (t ^ (t >>> 7)) & 0xFFFFFFFFL;
            long term4 = (t | 61L) & 0xFFFFFFFFL;
            t = (t ^ (t + term3 * term4)) & 0xFFFFFFFFL;

            long result = (t ^ (t >>> 14)) & 0xFFFFFFFFL;
            return (double) result / 4294967296.0;
        };
    }

    private record PuzzleWordData(DictionaryWord dictEntity, boolean isPangram, int score) {
    }

    private record GeneratedBoard(
            String centerLetter,
            List<String> uniqueLetters,
            List<PuzzleWordData> words,
            String seed) {
    }
}