import { Injectable } from '@angular/core';
import { GameState } from '../../models/game-state.model';

const STORAGE_PREFIX = 'beesagono:game:';
const CURRENT_SCHEMA_VERSION = 1;

@Injectable({ providedIn: 'root' })
export class StorageService {
  /** In-memory fallback store for cases where localStorage is blocked or full. */
  private inMemoryFallback = new Map<string, string>();

  /** Track whether the actual localStorage is usable or if we are using the fallback. */
  private storageAvailable = true;

  constructor() {
    this.storageAvailable = this.checkLocalStorageAvailability();
  }

  /** Attempts to persist state; returns false (never throws) on failure, triggering in-memory fallback in GameService */
  save(state: GameState): boolean {
    const key = `${STORAGE_PREFIX}${state.date}`;
    const payload = JSON.stringify(state);

    if (this.storageAvailable) {
      try {
        localStorage.setItem(key, payload);
        return true;
      } catch (error) {
        console.warn(
          'StorageService: Unable to save to localStorage (quota exceeded or blocked). Falling back to in-memory storage.',
          error
        );
        this.storageAvailable = false;
      }
    }

    this.inMemoryFallback.set(key, payload);
    return false;
  }

  /** Attempts to read state for a given date; returns null if absent, unreadable, or version-mismatched. */
  load(date: string): GameState | null {
    const key = `${STORAGE_PREFIX}${date}`;
    let rawData: string | null = null;

    if (this.storageAvailable) {
      try {
        rawData = localStorage.getItem(key);
      } catch (error) {
        console.warn('StorageService: Error reading from localStorage. Using fallback store.', error);
        this.storageAvailable = false;
      }
    }

    if (!rawData) {
      rawData = this.inMemoryFallback.get(key) ?? null;
    }

    if (!rawData) {
      return null;
    }

    try {
      const parsed = JSON.parse(rawData) as GameState;

      // Discards data if the version differs
      if (!parsed || typeof parsed !== 'object' || parsed.version !== CURRENT_SCHEMA_VERSION) {
        console.warn(
          `StorageService: Schema version mismatch for date ${date}. Discarding stored data.`
        );
        this.clear(date);
        return null;
      }

      return parsed;
    } catch (error) {
      console.error(`StorageService: Invalid JSON payload for key ${key}.`, error);
      return null;
    }
  }

  /** Removes a stored entry for a given date (used when rolling over to a new day). */
  clear(date: string): void {
    const key = `${STORAGE_PREFIX}${date}`;

    if (this.storageAvailable) {
      try {
        localStorage.removeItem(key);
      } catch (error) {
        console.warn('StorageService: Error removing item from localStorage.', error);
      }
    }

    this.inMemoryFallback.delete(key);
  }

  /** True if the last read/write succeeded via real localStorage; false if the service is running on the in-memory fallback. */
  isAvailable(): boolean {
    return this.storageAvailable;
  }

  /** Initial check to verify if localStorage is accessible. */
  private checkLocalStorageAvailability(): boolean {
    try {
      const testKey = `${STORAGE_PREFIX}test`;
      localStorage.setItem(testKey, '1');
      localStorage.removeItem(testKey);
      return true;
    } catch {
      return false;
    }
  }
}