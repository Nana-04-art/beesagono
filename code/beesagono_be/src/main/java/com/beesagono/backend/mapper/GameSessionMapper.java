package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.entity.FoundWord;
import com.beesagono.backend.entity.GameSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface GameSessionMapper {

    @Mapping(target = "puzzleId", source = "puzzle.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "foundWords", source = "foundWords", qualifiedByName = "mapFoundWordsToSet")
    GameSessionResponse toGameSessionResponse(GameSession gameSession);

    @Named("mapFoundWordsToSet")
    default Set<String> mapFoundWordsToSet(List<FoundWord> foundWords) {
        if (foundWords == null || foundWords.isEmpty()) {
            return Collections.emptySet();
        }
        return foundWords.stream()
                .filter(fw -> fw.getId() != null && fw.getId().getWord() != null)
                .map(fw -> fw.getId().getWord())
                .collect(Collectors.toSet());
    }
}