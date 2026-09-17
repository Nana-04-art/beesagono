package com.beesagono.backend.service;

import com.beesagono.backend.dto.auth.CreateAdminRequest;
import com.beesagono.backend.dto.auth.UserResponse;
import com.beesagono.backend.dto.dictionary.InvalidWordAttemptStatResponse;
import com.beesagono.backend.dto.puzzle.PuzzleAdminResponse;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleLettersRequest;
import com.beesagono.backend.dto.puzzle.UpdatePuzzleWordsRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for administrative operations including user management,
 * daily puzzle generation and modification, and dictionary auditing.
 */
public interface AdminService {

    // --- Admin User Management ---

    /**
     * Creates a new administrator account.
     *
     * @param request payload containing new administrator credentials and details
     * @return {@link UserResponse} with created admin profile information
     */
    UserResponse createAdmin(CreateAdminRequest request);

    /**
     * Retrieves a paginated list of system users, optionally filtered by username or email search term.
     *
     * @param search   optional search query to filter users
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of {@link UserResponse} items
     */
    Page<UserResponse> getUsers(String search, Pageable pageable);

    // --- Puzzle Management & Inspection ---

    /**
     * Generates or resets a future daily puzzle instance for a specified date.
     *
     * @param date target date for puzzle generation or reset
     * @return {@link PuzzleAdminResponse} detailing the generated or updated puzzle configuration
     */
    PuzzleAdminResponse generateOrResetFuturePuzzle(LocalDate date);

    /**
     * Retrieves comprehensive administrative details for a daily puzzle on a given date.
     *
     * @param date date of the puzzle to inspect
     * @return {@link PuzzleAdminResponse} containing puzzle statistics and full solution words
     */
    PuzzleAdminResponse getPuzzleDetailsByDate(LocalDate date);

    /**
     * Retrieves a summary list of all daily puzzles managed by the system.
     *
     * @return list of {@link PuzzleAdminResponse} summaries
     */
    List<PuzzleAdminResponse> getAllPuzzlesOverview();

    /**
     * Manually updates the set of valid solution words associated with a daily puzzle.
     *
     * @param puzzleId unique identifier of the target puzzle
     * @param request  payload containing sets of words to add or remove
     * @return updated {@link PuzzleAdminResponse}
     */
    PuzzleAdminResponse updatePuzzleWords(String puzzleId, UpdatePuzzleWordsRequest request);

    /**
     * Manually updates the central and outer letter configuration of a daily puzzle.
     *
     * @param puzzleId unique identifier of the target puzzle
     * @param request  payload containing new center and outer letter choices
     * @return updated {@link PuzzleAdminResponse}
     */
    PuzzleAdminResponse updatePuzzleLetters(String puzzleId, UpdatePuzzleLettersRequest request);

    // --- Audit & Dictionary ---

    /**
     * Retrieves aggregated frequency statistics for most commonly attempted invalid words.
     *
     * @return list of {@link InvalidWordAttemptStatResponse} items
     */
    List<InvalidWordAttemptStatResponse> getTopSuggestedWordsFromAttempts();

    /**
     * Removes a specified word entry completely from the system dictionary.
     *
     * @param word the word string to purge
     */
    void removeWordFromDictionary(String word);
}