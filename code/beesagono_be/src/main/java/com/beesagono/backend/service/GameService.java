package com.beesagono.backend.service;

import java.util.List;

import com.beesagono.backend.dto.game.GameBulkSyncRequest;
import com.beesagono.backend.dto.game.GameSessionResponse;
import com.beesagono.backend.dto.game.SubmitWordRequest;
import com.beesagono.backend.dto.game.SubmitWordResponse;

public interface GameService {

    GameSessionResponse getOrCreateTodaySession(String userId);

    SubmitWordResponse validateAndScoreWord(SubmitWordRequest request, String userId);

    List<GameSessionResponse> syncLocalProgress(GameBulkSyncRequest request, String userId);
}