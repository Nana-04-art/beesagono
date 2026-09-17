package com.beesagono.backend.mapper;

import com.beesagono.backend.dto.dictionary.DictionaryWordResponse;
import com.beesagono.backend.entity.DictionaryWord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DictionaryWordMapper {

    @Mapping(target = "addedByUsername", source = "addedByUser.username")
    @Mapping(target = "wordLength", expression = "java(dictionaryWord.getWord() != null ? dictionaryWord.getWord().length() : null)")
    DictionaryWordResponse toDictionaryWordResponse(DictionaryWord dictionaryWord);
}