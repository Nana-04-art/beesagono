package com.beesagono.backend.dto.game;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing a bulk synchronization request for offline or local game sessions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameBulkSyncRequest {
    @NotEmpty
    private List<GameSyncRequest> games;
}