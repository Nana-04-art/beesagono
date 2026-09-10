package com.beesagono.backend.dto.game;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameBulkSyncRequest {
    @NotEmpty
    private List<GameSyncRequest> games;
}