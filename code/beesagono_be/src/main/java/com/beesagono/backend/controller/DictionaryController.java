package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.*;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.DictionaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/admin/dictionary")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DictionaryController {

    private final DictionaryService dictionaryService;
    private final UserRepository userRepository;

    @PostMapping("/word")
    public ResponseEntity<DictionaryWordResponse> addSingleWord(
            @Valid @RequestBody AddWordRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User admin = getAdminUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(dictionaryService.addSingleWord(request, admin));
    }

    @PostMapping("/words/batch")
    public ResponseEntity<BatchUploadResponse> addBatchWords(
            @Valid @RequestBody BatchAddWordRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User admin = getAdminUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(dictionaryService.addBatchWords(request, admin));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchUploadResponse> uploadFromFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User admin = getAdminUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dictionaryService.uploadWordsFromFile(file, admin));
    }

    private User getAdminUser(UserDetailsImpl userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente admin non trovato."));
    }

    @GetMapping
    public ResponseEntity<Page<DictionaryWordResponse>> getWords(
            @ModelAttribute DictionaryFilterRequest filterRequest,
            @PageableDefault(size = 20, sort = "word", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<DictionaryWordResponse> page = dictionaryService.getWords(filterRequest, pageable);
        return ResponseEntity.ok(page);
    }
}