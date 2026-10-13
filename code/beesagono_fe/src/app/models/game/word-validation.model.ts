export interface WordValidationRequest {
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