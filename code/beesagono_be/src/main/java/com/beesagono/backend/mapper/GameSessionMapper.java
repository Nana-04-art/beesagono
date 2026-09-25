package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.entity.FoundWord;
import com.beesagono.backend.entity.GameSession;
import com.beesagono.backend.entity.InvalidWordAttempt;
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
    @Mapping(target = "invalidWords", source = "invalidWordAttempts", qualifiedByName = "mapInvalidWordsToSet")
    @Mapping(target = "foundMielegrammi", source = "foundWords", qualifiedByName = "mapFoundMielegrammiToSet")
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

    @Named("mapInvalidWordsToSet")
    default Set<String> mapInvalidWordsToSet(List<InvalidWordAttempt> invalidWordAttempts) {
        if (invalidWordAttempts == null || invalidWordAttempts.isEmpty()) {
            return Collections.emptySet();
        }
        return invalidWordAttempts.stream()
                .filter(iwa -> iwa.getAttemptedWord() != null)
                .map(InvalidWordAttempt::getAttemptedWord)
                .collect(Collectors.toSet());
    }

    @Named("mapFoundMielegrammiToSet")
    default Set<String> mapFoundMielegrammiToSet(List<FoundWord> foundWords) {
        if (foundWords == null || foundWords.isEmpty()) {
            return Collections.emptySet();
        }
        return foundWords.stream()
                .filter(fw -> Boolean.TRUE.equals(fw.getIsMielegramma()))
                .filter(fw -> fw.getId() != null && fw.getId().getWord() != null)
                .map(fw -> fw.getId().getWord())
                .collect(Collectors.toSet());
    }
}