package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.WordValidationRequest;
import com.beesagono.backend.dto.dictionary.WordValidationResponse;
import com.beesagono.backend.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Dictionary & Guest Gameplay", description = "Endpoints for word validation and public dictionary services for guest users")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @Operation(summary = "Validate a word for Guest users", description = "Validates a submitted word for unauthenticated/guest users against the specified daily puzzle constraints (minimum length, mandatory center letter, dictionary presence) and calculates the earned score.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation outcome computed successfully (for both valid words and game-rule rejections)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WordValidationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed request body or missing required input fields", content = @Content),
            @ApiResponse(responseCode = "404", description = "Puzzle not found for the specified date", content = @Content)
    })
    @PostMapping("/validate")
    public ResponseEntity<WordValidationResponse> validateWordForGuest(@RequestBody WordValidationRequest request) {
        return ResponseEntity.ok(dictionaryService.validateWordForGuest(request.getWord()));
    }
}