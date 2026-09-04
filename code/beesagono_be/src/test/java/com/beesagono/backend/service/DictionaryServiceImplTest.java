package com.beesagono.backend.service;

import com.beesagono.backend.dto.dictionary.*;
import com.beesagono.backend.entity.DictionaryWord;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.mapper.DictionaryWordMapper;
import com.beesagono.backend.repository.DictionaryWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceImplTest {

    @Mock
    private DictionaryWordRepository dictionaryWordRepository;

    @Mock
    private DictionaryWordMapper dictionaryWordMapper;

    @InjectMocks
    private DictionaryServiceImpl dictionaryService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id("admin-1").username("admin").build();
    }

    @Test
    @DisplayName("addSingleWord - Success")
    void addSingleWord_Success() {
        AddWordRequest request = new AddWordRequest();
        request.setWord("àlbero");

        DictionaryWord savedWord = DictionaryWord.builder()
                .word("ALBERO")
                .wordLength(6)
                .uniqueLettersCount(5)
                .isCandidatePangram(false)
                .addedByUser(adminUser)
                .build();

        DictionaryWordResponse expectedResponse = DictionaryWordResponse.builder()
                .word("ALBERO")
                .wordLength(6)
                .uniqueLettersCount(5)
                .isCandidatePangram(false)
                .build();

        when(dictionaryWordRepository.existsById("ALBERO")).thenReturn(false);
        when(dictionaryWordRepository.save(any(DictionaryWord.class))).thenReturn(savedWord);
        when(dictionaryWordMapper.toDictionaryWordResponse(savedWord)).thenReturn(expectedResponse);

        DictionaryWordResponse response = dictionaryService.addSingleWord(request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getWord()).isEqualTo("ALBERO");
        verify(dictionaryWordRepository, times(1)).save(any(DictionaryWord.class));
    }

    @Test
    @DisplayName("addSingleWord - Throws Exception when word length < 4")
    void addSingleWord_TooShort() {
        AddWordRequest request = new AddWordRequest();
        request.setWord("SOL");

        assertThatThrownBy(() -> dictionaryService.addSingleWord(request, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("almeno 4 lettere");
    }

    @Test
    @DisplayName("addSingleWord - Throws Exception when word already exists")
    void addSingleWord_AlreadyExists() {
        AddWordRequest request = new AddWordRequest();
        request.setWord("CASA");

        when(dictionaryWordRepository.existsById("CASA")).thenReturn(true);

        assertThatThrownBy(() -> dictionaryService.addSingleWord(request, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("già presente nel dizionario");
    }

    @Test
    @DisplayName("addSingleWord - Throws Exception when unique letters > 7")
    void addSingleWord_TooManyUniqueLetters() {
        AddWordRequest request = new AddWordRequest();
        request.setWord("ABCDEFGHI");

        assertThatThrownBy(() -> dictionaryService.addSingleWord(request, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("più di 7 lettere uniche");
    }

    @Test
    @DisplayName("addBatchWords - Success")
    void addBatchWords_Success() {
        BatchAddWordRequest request = new BatchAddWordRequest();
        request.setWords(List.of("casa", "albero", "duplicata"));

        when(dictionaryWordRepository.findAllById(any())).thenReturn(List.of(
                DictionaryWord.builder().word("DUPLICATA").build()
        ));

        BatchUploadResponse response = dictionaryService.addBatchWords(request, adminUser);

        assertThat(response.getTotalProcessed()).isEqualTo(3);
        assertThat(response.getAddedCount()).isEqualTo(2);
        assertThat(response.getSkippedCount()).isEqualTo(1);
        verify(dictionaryWordRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("uploadWordsFromFile - Success")
    void uploadWordsFromFile_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "casa, albero\nfiore".getBytes()
        );

        when(dictionaryWordRepository.findAllById(any())).thenReturn(Collections.emptyList());

        BatchUploadResponse response = dictionaryService.uploadWordsFromFile(file, adminUser);

        assertThat(response.getTotalProcessed()).isEqualTo(3);
        assertThat(response.getAddedCount()).isEqualTo(3);
        verify(dictionaryWordRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("uploadWordsFromFile - Empty file throws Exception")
    void uploadWordsFromFile_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> dictionaryService.uploadWordsFromFile(file, adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("file inviato è vuoto");
    }

    @Test
    @DisplayName("getWords - Success")
    @SuppressWarnings("unchecked")
    void getWords_Success() {
        DictionaryFilterRequest filterRequest = new DictionaryFilterRequest();
        Pageable pageable = Pageable.unpaged();

        DictionaryWord entity = DictionaryWord.builder().word("CASA").build();
        DictionaryWordResponse responseDto = DictionaryWordResponse.builder().word("CASA").build();
        Page<DictionaryWord> page = new PageImpl<>(List.of(entity));

        when(dictionaryWordRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(dictionaryWordMapper.toDictionaryWordResponse(entity)).thenReturn(responseDto);

        Page<DictionaryWordResponse> result = dictionaryService.getWords(filterRequest, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getWord()).isEqualTo("CASA");
    }
}