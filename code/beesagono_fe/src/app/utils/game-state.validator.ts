import { GameState } from '../models/game-state.model';

/**
 * Validates that a string is a valid ISO 8601 date (YYYY-MM-DD),
 * also checking the correctness of months, days, and leap years on the calendar.
 */
export function isValidIsoDate(dateStr: unknown): dateStr is string {
    if (typeof dateStr !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
        return false;
    }
    const [year, month, day] = dateStr.split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 1, day));

    return (
        date.getUTCFullYear() === year &&
        date.getUTCMonth() === month - 1 &&
        date.getUTCDate() === day
    );
}

//  Strict Type Guard to verify that an unknown object conforms to the GameState interface.
export function isValidGameState(obj: unknown): obj is GameState {
    if (!obj || typeof obj !== 'object' || Array.isArray(obj)) {
        return false;
    }

    const state = obj as Record<string, unknown>;

    if (state['version'] !== 1) return false;
    if (!isValidIsoDate(state['date'])) return false;
    if (typeof state['score'] !== 'number' || state['score'] < 0 || !Number.isFinite(state['score'])) return false;
    if (typeof state['isCompleted'] !== 'boolean') return false;
    if (!Array.isArray(state['foundWords']) || !state['foundWords'].every(w => typeof w === 'string')) return false;
    if (!Array.isArray(state['invalidWords']) || !state['invalidWords'].every(w => typeof w === 'string')) return false;

    // rankLabel must be a string (even empty) or null
    if (state['rankLabel'] !== null && typeof state['rankLabel'] !== 'string') return false;

    return true;
}