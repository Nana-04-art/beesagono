package com.beesagono.backend.enums;

/**
 * Enum defining error codes for invalid word submission attempts during a game session.
 */
public enum ErrorTypeCode {
    TOO_SHORT,
    MISSING_CENTER,
    INVALID_LETTERS,
    ALREADY_FOUND,
    NOT_IN_DICTIONARY,
    NOT_IN_PUZZLE
}