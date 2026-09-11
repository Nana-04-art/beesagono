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
@Tag(name = "Dictionary Controller", description = "Endpoints for dictionary management, batch imports, text file processing, and invalid attempt audits")
public class DictionaryController {

        private final DictionaryService dictionaryService;
        private final UserRepository userRepository;
        private final AdminService adminService;

        @Operation(summary = "Add a single word to the dictionary", description = "Adds a single new valid word along with its metadata into the system dictionary.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Word added to dictionary successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DictionaryWordResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid word payload or validation error", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Admin user not found", content = @Content),
                        @ApiResponse(responseCode = "409", description = "Word already exists in the dictionary", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error while adding word", content = @Content)
        })
        @PostMapping("/word")
        public ResponseEntity<DictionaryWordResponse> addWordToDictionary(
                        @Valid @RequestBody AddWordRequest request,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
                User admin = getAdminUser(userDetails);
                DictionaryWordResponse response = dictionaryService.addSingleWord(request, admin);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @Operation(summary = "Insert a list of words in batch mode", description = "Allows bulk insertion of multiple dictionary words in a single request payload.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Batch insertion processed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BatchUploadResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid payload structure or empty word list", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Admin user not found", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error during batch processing", content = @Content)
        })
        @PostMapping("/words/batch")
        public ResponseEntity<BatchUploadResponse> addBatchWords(
                        @Valid @RequestBody BatchAddWordRequest request,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
                User admin = getAdminUser(userDetails);
                return ResponseEntity.status(HttpStatus.CREATED).body(dictionaryService.addBatchWords(request, admin));
        }

        @Operation(summary = "Upload and import words from a text file", description = "Parses a multipart text file containing words line-by-line and imports them into the system dictionary.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "File processed and words imported successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BatchUploadResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Missing or unsupported file format", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Admin user not found", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error during file processing", content = @Content)
        })
        @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<BatchUploadResponse> uploadFromFile(
                        @Parameter(description = "Text file (.txt) containing line-separated words to import", required = true) @RequestParam("file") MultipartFile file,
                        @Parameter(hidden = true) @AuthenticationPrincipal UserDetailsImpl userDetails) {
                User admin = getAdminUser(userDetails);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(dictionaryService.uploadWordsFromFile(file, admin));
        }

        @Operation(summary = "Retrieve a paginated and filtered list of dictionary words", description = "Fetches dictionary entries with options to filter by prefix, minimum length, or validity status.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Paginated dictionary words retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid filter parameters", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error while retrieving words", content = @Content)
        })
        @GetMapping
        public ResponseEntity<Page<DictionaryWordResponse>> getWords(
                        @ModelAttribute DictionaryFilterRequest filterRequest,
                        @Parameter(description = "Pagination parameters (page, size, sort)") @PageableDefault(size = 20, sort = "word", direction = Sort.Direction.ASC) Pageable pageable) {
                return ResponseEntity.ok(dictionaryService.getWords(filterRequest, pageable));
        }

        @Operation(summary = "Statistics on the most played invalid words by users", description = "Returns aggregated statistical metrics regarding invalid words attempted by players, useful for dictionary expansion candidates.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invalid word statistics retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = InvalidWordAttemptStatResponse.class)))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error while generating attempt statistics", content = @Content)
        })
        @GetMapping("/invalid-attempts")
        public ResponseEntity<List<InvalidWordAttemptStatResponse>> getTopSuggestedWords() {
                return ResponseEntity.ok(adminService.getTopSuggestedWordsFromAttempts());
        }

        @Operation(summary = "Permanently remove a word from the dictionary", description = "Deletes a specific word entry permanently from the system dictionary.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Word deleted successfully (No Content)"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized user", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Word not found in dictionary", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error during word removal", content = @Content)
        })
        @DeleteMapping("/word/{word}")
        public ResponseEntity<Void> removeWordFromDictionary(
                        @Parameter(description = "Word to be removed from the dictionary", required = true, example = "CASA") @PathVariable String word) {
                adminService.removeWordFromDictionary(word);
                return ResponseEntity.noContent().build();
        }

        private User getAdminUser(UserDetailsImpl userDetails) {
                return userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Utente admin non trovato."));
        }
}