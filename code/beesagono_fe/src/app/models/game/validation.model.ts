/** Frozen, final set of validation error codes — must match toast copy exactly. */
export type ValidationErrorType =
    | 'TOO_SHORT'
    | 'MISSING_CENTER'
    | 'INVALID_LETTERS'
    | 'ALREADY_FOUND'
    | 'NOT_IN_DICTIONARY';

export interface ValidationResult {
    isValid: boolean;
    pointsAwarded: number;
    isMielegramma: boolean;
    errorType?: ValidationErrorType;
    message?: string;
}