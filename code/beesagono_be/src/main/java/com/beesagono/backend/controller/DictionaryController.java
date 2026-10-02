package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.WordValidationRequest;
import com.beesagono.backend.dto.dictionary.WordValidationResponse;
import com.beesagono.backend.service.DictionaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @PostMapping("/validate")
    public ResponseEntity<WordValidationResponse> validateWordGuest(
            @Valid @RequestBody WordValidationRequest request) {

        WordValidationResponse response = dictionaryService.validateWordForGuest(
                request.getPuzzleDate(),
                request.getWord());

        return ResponseEntity.ok(response);
    }
}