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

public interface AdminService {

    // --- Admin User Management ---
    UserResponse createAdmin(CreateAdminRequest request);

    Page<UserResponse> getUsers(String search, Pageable pageable);

    // --- Puzzle Management & Inspection ---
    PuzzleAdminResponse generateOrResetFuturePuzzle(LocalDate date);

    PuzzleAdminResponse getPuzzleDetailsByDate(LocalDate date);

    List<PuzzleAdminResponse> getAllPuzzlesOverview();

    PuzzleAdminResponse updatePuzzleWords(String puzzleId, UpdatePuzzleWordsRequest request);

    PuzzleAdminResponse updatePuzzleLetters(String puzzleId, UpdatePuzzleLettersRequest request);

    // --- Audit & Dictionary ---
    List<InvalidWordAttemptStatResponse> getTopSuggestedWordsFromAttempts();

    void removeWordFromDictionary(String word);
}