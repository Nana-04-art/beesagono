package com.beesagono.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.beesagono.backend.dto.dictionary.AddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchAddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchUploadResponse;
import com.beesagono.backend.dto.dictionary.DictionaryFilterRequest;
import com.beesagono.backend.dto.dictionary.DictionaryWordResponse;
import com.beesagono.backend.entity.User;

public interface DictionaryService {

    DictionaryWordResponse addSingleWord(AddWordRequest request, User adminUser);

    BatchUploadResponse addBatchWords(BatchAddWordRequest request, User adminUser);

    BatchUploadResponse uploadWordsFromFile(MultipartFile file, User adminUser);

    Page<DictionaryWordResponse> getWords(DictionaryFilterRequest filterRequest, Pageable pageable);
}
