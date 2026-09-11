package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.WordValidationRequest;
import com.beesagono.backend.dto.dictionary.WordValidationResponse;
import com.beesagono.backend.service.DictionaryService;
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
    public ResponseEntity<WordValidationResponse> validateWordForGuest(@RequestBody WordValidationRequest request) {
        return ResponseEntity.ok(dictionaryService.validateWordForGuest(request.getWord()));
    }
}