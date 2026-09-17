package com.beesagono.backend.controller;

import com.beesagono.backend.dto.dictionary.AddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchAddWordRequest;
import com.beesagono.backend.dto.dictionary.BatchUploadResponse;
import com.beesagono.backend.dto.dictionary.DictionaryFilterRequest;
import com.beesagono.backend.dto.dictionary.DictionaryWordResponse;
import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStatResponse;
import com.beesagono.backend.entity.User;
import com.beesagono.backend.repository.UserRepository;
import com.beesagono.backend.security.UserDetailsImpl;
import com.beesagono.backend.service.AdminService;
import com.beesagono.backend.service.DictionaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dictionary")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dictionary Management", description = "Endpoints for managing global dictionary entries, batch imports, file uploads, and invalid attempt statistics")
@SecurityRequirement(name = "bearerAuth")
public class AdminDictionaryController {

    private final DictionaryService dictionaryService;
    private final UserRepository userRepository;
    private final AdminService adminService;

    @Operation(summary = "Add a single word to the dictionary", description = "Adds a single new word with its metadata to the global dictionary. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Word added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DictionaryWordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation error or word already exists in dictionary", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    @PostMapping("/word")
    public ResponseEntity<DictionaryWordResponse> addWordToDictionary(
            @RequestBody @Valid AddWordRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User admin = getAdminUser(userDetails);
        DictionaryWordResponse response = dictionaryService.addSingleWord(request, admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Add a batch of words to the dictionary", description = "Performs a bulk insertion of multiple words into the global dictionary. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Batch process completed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BatchUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation failure or empty payload", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    @PostMapping("/words/batch")
    public ResponseEntity<BatchUploadResponse> addBatchWords(
            @Valid @RequestBody BatchAddWordRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User admin = getAdminUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED).body(dictionaryService.addBatchWords(request, admin));
    }

    @Operation(summary = "Upload words from a file", description = "Imports dictionary words in bulk by uploading a text/CSV file. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File processed and words imported successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BatchUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid or unreadable file format", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchUploadResponse> uploadFromFile(
            @Parameter(description = "Multipart file containing words to import (.txt, .csv)", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User admin = getAdminUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dictionaryService.uploadWordsFromFile(file, admin));
    }

    @Operation(summary = "Retrieve paginated list of dictionary words", description = "Fetches a paginated and optionally filtered list of words currently stored in the global dictionary. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dictionary words page retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<DictionaryWordResponse>> getWords(
            @ModelAttribute DictionaryFilterRequest filterRequest,
            @Parameter(description = "Pagination and sorting parameters (e.g., page=0, size=20, sort=word,asc)") @PageableDefault(size = 20, sort = "word", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<DictionaryWordResponse> page = dictionaryService.getWords(filterRequest, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Get top invalid word attempts statistics", description = "Retrieves statistics on the most frequently submitted invalid words across game sessions to help admins spot potential dictionary omissions. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = InvalidWordAttemptStatResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    @GetMapping("/invalid-attempts")
    public ResponseEntity<List<InvalidWordAttemptStatResponse>> getTopSuggestedWords() {
        return ResponseEntity.ok(adminService.getTopSuggestedWordsFromAttempts());
    }

    @Operation(summary = "Remove a word from the dictionary", description = "Deletes a word from the global dictionary by name. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Word removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @ApiResponse(responseCode = "404", description = "Word not found in dictionary", content = @Content)
    })
    @DeleteMapping("/word/{word}")
    public ResponseEntity<Void> removeWordFromDictionary(
            @Parameter(description = "The target word string to remove", example = "PAROLA") @PathVariable String word) {

        adminService.removeWordFromDictionary(word);
        return ResponseEntity.noContent().build();
    }

    private User getAdminUser(UserDetailsImpl userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente admin non trovato."));
    }
}