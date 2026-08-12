import { Injectable } from '@angular/core';
import { GameState } from '../../models/game-state.model';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly PREFIX = 'beesagono:game:';
  /** In-memory fallback store for cases where localStorage is blocked or full. */
  private inMemoryFallback = new Map<string, string>();
  private usingFallback = false;

  /**
   * Persists the GameState object to localStorage or falls back to in-memory store.
   */
  save(state: GameState): boolean {
    if (!state || !state.date) {
      console.warn('[StorageService] Attempted to save an invalid game state.');
      return false;
    }

    const key = `${this.PREFIX}${state.date}`;
    const payload = JSON.stringify(state);

    try {
      localStorage.setItem(key, payload);
      // If successful, clear any fallback data for this key
      this.inMemoryFallback.delete(key);
      this.usingFallback = false;
      return true;
    } catch (error) {
      console.warn('[StorageService] Unable to save to localStorage (quota exceeded or blocked).', error);
      this.inMemoryFallback.set(key, payload);
      this.usingFallback = true;
      return false;
    }
  }

  /**
   * Reads and validates the GameState object for a given date.
   */
  load(date: string): GameState | null {
    const key = `${this.PREFIX}${date}`;

    try {
      // Attempt to read from inMemoryFallback first
      let raw = this.inMemoryFallback.get(key);
      // If not found in fallback, try localStorage
      if (!raw) {
        raw = localStorage.getItem(key) ?? undefined;
      }
      if (!raw) return null;
      const parsed = JSON.parse(raw);

      // Structure and Schema Version validation (§10.3)
      if (!this.isValidGameState(parsed, date)) {
        console.warn('[StorageService] Corrupted or mismatched saved state.');
        return null;
      }

      // Sanitize and deduplicate invalidWords if present
      if (Array.isArray(parsed.invalidWords)) {
        parsed.invalidWords = Array.from(
          new Set(
            parsed.invalidWords.filter((item: unknown): item is string => typeof item === 'string')
          )
        );
      } else {
        parsed.invalidWords = [];
      }

      return parsed as GameState;
    } catch (error) {
      console.error('[StorageService] Error occurred while parsing the saved state:', error);
      // Attempt to recover from in-memory fallback if available
      const fallbackRaw = this.inMemoryFallback.get(key);
      if (fallbackRaw) {
        try {
          const parsedFallback = JSON.parse(fallbackRaw);
          if (this.isValidGameState(parsedFallback, date)) {
            if (Array.isArray(parsedFallback.invalidWords)) {
              parsedFallback.invalidWords = Array.from(
                new Set(
                  parsedFallback.invalidWords.filter(
                    (item: unknown): item is string => typeof item === 'string'
                  )
                )
              );
            } else {
              parsedFallback.invalidWords = [];
            }
            return parsedFallback as GameState;
          }
        } catch {
          // Fallback parsing failed
        }
      }
      return null;
    }
  }

  /**
   * Removes a stored entry for a given date.
   */
  clear(date: string): void {
    const key = `${this.PREFIX}${date}`;
    try {
      localStorage.removeItem(key);
    } catch {
      // Ignored
    }
    this.inMemoryFallback.delete(key);
  }

  /**
   * Returns true if running natively with localStorage.
   */
  isAvailable(): boolean {
    return !this.usingFallback;
  }

  /**
   * Defensive validation check for loaded objects.
   */
  private isValidGameState(obj: any, expectedDate: string): obj is GameState {
    const isInvalidWordsValid =
      obj.invalidWords === undefined ||
      (Array.isArray(obj.invalidWords) &&
        obj.invalidWords.every((item: unknown) => typeof item === 'string'));

    return (
      obj &&
      typeof obj === 'object' &&
      obj.version === 1 &&
      obj.date === expectedDate &&
      Array.isArray(obj.foundWords) &&
      typeof obj.startTime === 'number' &&
      typeof obj.lastUpdated === 'number' &&
      isInvalidWordsValid
    );
  }
}