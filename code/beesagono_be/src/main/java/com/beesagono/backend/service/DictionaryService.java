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

/**
 * Service interface for managing dictionary entries, bulk word processing, and file uploads.
 */
public interface DictionaryService {

    /**
     * Adds a single new word entry to the global system dictionary.
     *
     * @param request   payload containing word details and pangram eligibility flag
     * @param adminUser administrative user performing the action
     * @return {@link DictionaryWordResponse} representing the newly inserted word
     */
    DictionaryWordResponse addSingleWord(AddWordRequest request, User adminUser);

    /**
     * Performs a batch insertion of multiple words into the global dictionary.
     *
     * @param request   payload containing list of words to import
     * @param adminUser administrative user performing the operation
     * @return {@link BatchUploadResponse} summarizing inserted and skipped item counts
     */
    BatchUploadResponse addBatchWords(BatchAddWordRequest request, User adminUser);

    /**
     * Parses a text file containing words line-by-line and imports valid entries into the dictionary.
     *
     * @param file      uploaded file containing raw word strings
     * @param adminUser administrative user performing the upload
     * @return {@link BatchUploadResponse} detailing file processing outcomes
     */
    BatchUploadResponse uploadWordsFromFile(MultipartFile file, User adminUser);

    /**
     * Fetches a paginated and filtered list of words from the system dictionary.
     *
     * @param filterRequest filter criteria including search strings, exact lengths, or pangram flags
     * @param pageable      pagination and sorting settings
     * @return {@link Page} of {@link DictionaryWordResponse} items matching criteria
     */
    Page<DictionaryWordResponse> getWords(DictionaryFilterRequest filterRequest, Pageable pageable);
}