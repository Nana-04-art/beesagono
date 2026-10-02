package com.beesagono.backend.dto.dictionary;

public interface InvalidWordAttemptStat {
    String getAttemptedWord();

    Long getAttemptCount();
}