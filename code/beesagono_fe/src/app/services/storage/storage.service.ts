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
    try {
      const payload = JSON.stringify(state);
      localStorage.setItem(key, payload);
      this.usingFallback = false;
      return true;
    } catch (error) {
      console.warn('[StorageService] Unable to save to localStorage (quota exceeded or blocked).', error);
      this.inMemoryFallback.set(key, JSON.stringify(state));
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
      const raw = localStorage.getItem(key) ?? this.inMemoryFallback.get(key);
      if (!raw) return null;

      const parsed = JSON.parse(raw);

      // Structure and Schema Version validation (§10.3)
      if (!this.isValidGameState(parsed, date)) {
        console.warn('[StorageService] Corrupted or mismatched saved state.');
        return null;
      }

      return parsed as GameState;
    } catch (error) {
      console.error('[StorageService] Error occurred while parsing the saved state:', error);
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
    return (
      obj &&
      typeof obj === 'object' &&
      obj.version === 1 &&
      obj.date === expectedDate &&
      Array.isArray(obj.foundWords) &&
      typeof obj.startTime === 'number' &&
      typeof obj.lastUpdated === 'number'
    );
  }
}