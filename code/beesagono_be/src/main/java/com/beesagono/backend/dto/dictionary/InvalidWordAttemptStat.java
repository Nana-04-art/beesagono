package com.beesagono.backend.dto.dictionary;

/**
 * Projection interface for aggregated query results on invalid word attempts,
 * tracking the frequency of specific rejected word submissions.
 */
public interface InvalidWordAttemptStat {
    String getAttemptedWord();

    Long getAttemptCount();
}