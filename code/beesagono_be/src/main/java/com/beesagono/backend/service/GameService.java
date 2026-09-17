package com.beesagono.backend.service;

import java.util.List;

import com.beesagono.backend.dto.game.GameBulkSyncRequest;
import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.dto.game.SubmitWordRequest;
import com.beesagono.backend.dto.game.SubmitWordResponse;

/**
 * Service interface managing active gameplay sessions, real-time word submission validation,
 * and offline progress synchronization.
 */
public interface GameService {

    /**
     * Retrieves an existing active game session for the current daily puzzle or initializes a new one.
     *
     * @param userId unique identifier of the player
     * @return {@link GameSessionResponse} representing current session status and found words
     */
    GameSessionResponse getOrCreateTodaySession(String userId);

    /**
     * Validates a player's word submission against puzzle rules and calculates points earned.
     *
     * @param request payload containing session ID and attempted word
     * @param userId  unique identifier of the submitting player
     * @return {@link SubmitWordResponse} containing validation status, earned points, and updated rank
     */
    SubmitWordResponse validateAndScoreWord(SubmitWordRequest request, String userId);

    /**
     * Synchronizes locally stored or offline gameplay progress with the backend server.
     *
     * @param request payload containing batch sync items for past sessions
     * @param userId  unique identifier of the player
     * @return list of synchronized {@link GameSessionResponse} items
     */
    List<GameSessionResponse> syncLocalProgress(GameBulkSyncRequest request, String userId);
}