package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DailyPuzzleMapper {

    @Mapping(target = "outerLetters", source = "outerLetters", qualifiedByName = "mapOuterLettersToSet")
    @Mapping(target = "totalWordsCount", expression = "java(dailyPuzzle.getPuzzleWords() != null ? dailyPuzzle.getPuzzleWords().size() : 0)")
    @Mapping(target = "totalMielegrammiCount", expression = "java(calculateMielegrammiCount(dailyPuzzle))")
    @Mapping(target = "wordsCountByLetter", expression = "java(calculateWordsCountByLetter(dailyPuzzle))")
    @Mapping(target = "mielegrammiCountByLetter", expression = "java(calculateMielegrammiCountByLetter(dailyPuzzle))")
    DailyPuzzleResponse toDailyPuzzleResponse(DailyPuzzle dailyPuzzle);

    @Named("mapOuterLettersToSet")
    default Set<String> mapOuterLettersToSet(List<PuzzleOuterLetter> outerLetters) {
        if (outerLetters == null || outerLetters.isEmpty()) {
            return Collections.emptySet();
        }
        return outerLetters.stream()
                .filter(ol -> ol.getId() != null && ol.getId().getLetter() != null)
                .map(ol -> ol.getId().getLetter())
                .collect(Collectors.toSet());
    }

    default Integer calculateMielegrammiCount(DailyPuzzle dailyPuzzle) {
        if (dailyPuzzle.getPuzzleWords() == null) {
            return 0;
        }
        return (int) dailyPuzzle.getPuzzleWords().stream()
                .filter(pw -> pw.getDictionaryWord() != null)
                .filter(pw -> Boolean.TRUE.equals(pw.getDictionaryWord().getIsCandidatePangram()))
                .count();
    }

    default Map<String, Integer> calculateWordsCountByLetter(DailyPuzzle dailyPuzzle) {
        if (dailyPuzzle.getPuzzleWords() == null) {
            return Collections.emptyMap();
        }
        return dailyPuzzle.getPuzzleWords().stream()
                .filter(pw -> pw.getId() != null && pw.getId().getWord() != null)
                .collect(Collectors.toMap(
                        pw -> pw.getId().getWord().substring(0, 1).toUpperCase(),
                        pw -> 1,
                        Integer::sum));
    }

    default Map<String, Integer> calculateMielegrammiCountByLetter(DailyPuzzle dailyPuzzle) {
        if (dailyPuzzle.getPuzzleWords() == null) {
            return Collections.emptyMap();
        }
        return dailyPuzzle.getPuzzleWords().stream()
                .filter(pw -> pw.getId() != null && pw.getId().getWord() != null)
                .filter(pw -> pw.getDictionaryWord() != null
                        && Boolean.TRUE.equals(pw.getDictionaryWord().getIsCandidatePangram()))
                .collect(Collectors.toMap(
                        pw -> pw.getId().getWord().substring(0, 1).toUpperCase(),
                        pw -> 1,
                        Integer::sum));
    }
}