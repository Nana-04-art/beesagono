package com.beesagono.backend.dto.dictionary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the execution summary after a bulk dictionary upload operation,
 * including processed, added, and skipped item counts.
 */
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