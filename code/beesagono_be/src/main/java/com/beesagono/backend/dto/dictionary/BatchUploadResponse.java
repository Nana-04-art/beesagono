package com.beesagono.backend.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {

    private int totalProcessed;
    private int addedCount;
    private int skippedCount;
    private String message;
}