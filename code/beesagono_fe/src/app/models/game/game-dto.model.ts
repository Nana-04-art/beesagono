import { ValidationErrorType } from "./validation.model";

export interface GameSessionResponse {
    id: string;
    puzzleId: string;
    userId: string;
    currentScore: number;
    currentRankLabel: string;
    isCompleted: boolean;
    foundWords: string[];
    invalidWords: string[];
    foundMielegrammi: string[];
    startTime: string;
    lastUpdated: string;
}

export interface SubmitWordRequest {
    sessionId: string;
    word: string;
}

export interface SubmitWordResponse {
    success: boolean;
    word: string;
    pointsEarned: number;
    currentScore: number;
    currentRankLabel: string;
    isMielegramma: boolean;
    errorCode?: ValidationErrorType | null;
    errorMessage?: string | null;
}

export interface WordValidationRequest {
    puzzleDate: string;
    word: string;
}

export interface WordValidationResponse {
    word: string;
    valid: boolean;
    pointsEarned: number;
    isMielegramma: boolean;
    errorCode?: string; // e.g. "NOT_IN_DICTIONARY", "MISSING_CENTER", "TOO_SHORT"
    errorMessage?: string;
}