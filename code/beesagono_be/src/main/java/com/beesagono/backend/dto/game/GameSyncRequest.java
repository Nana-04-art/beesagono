package com.beesagono.backend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSyncRequest {
    private LocalDate puzzleDate;
    private String centerLetter;
    private List<String> foundWords;
}