package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.puzzle.DailyPuzzleResponse;
import com.beesagono.backend.entity.DailyPuzzle;
import com.beesagono.backend.entity.PuzzleOuterLetter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DailyPuzzleMapper {

    @Mapping(target = "outerLetters", source = "outerLetters", qualifiedByName = "mapOuterLettersToSet")
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
}