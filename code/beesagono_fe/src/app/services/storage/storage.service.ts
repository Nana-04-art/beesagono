import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly DEFAULT_PREFIX = 'beesagono:';
  /** In-memory fallback store for cases where localStorage is blocked or full. */
  private inMemoryFallback = new Map<string, string>();
  private usingFallback = false;

  /**
   * Persists generic data to localStorage or falls back to in-memory store.
   */
  save<T>(key: string, data: T): boolean {
    if (!key || data === undefined || data === null) {
      console.warn('[StorageService] Attempted to save invalid key or data.');
      return false;
    }

    const fullKey = this.getFullKey(key);
    const payload = JSON.stringify(data);

    try {
      localStorage.setItem(fullKey, payload);
      this.inMemoryFallback.delete(fullKey);
      this.usingFallback = false;
      return true;
    } catch (error) {
      console.warn('[StorageService] Unable to save to localStorage (quota exceeded or blocked).', error);
      this.inMemoryFallback.set(fullKey, payload);
      this.usingFallback = true;
      return false;
    }
  }

  /**
   * Reads and parses data from in-memory fallback or localStorage.
   */
  load<T>(key: string): T | null {
    if (!key) return null;
    const fullKey = this.getFullKey(key);

    try {
      let raw = this.inMemoryFallback.get(fullKey);
      if (!raw) {
        raw = localStorage.getItem(fullKey) ?? undefined;
      }
      if (!raw) return null;

      return JSON.parse(raw) as T;
    } catch (error) {
      console.error('[StorageService] Error occurred while parsing saved data:', error);
      return null;
    }
  }

  /**
   * Removes a stored entry for a given key.
   */
  clear(key: string): void {
    const fullKey = this.getFullKey(key);
    try {
      localStorage.removeItem(fullKey);
    } catch {
      // Ignored
    }
    this.inMemoryFallback.delete(fullKey);
  }

  /**
   * Returns true if running natively with localStorage.
   */
  isAvailable(): boolean {
    return !this.usingFallback;
  }

  private getFullKey(key: string): string {
    return key.startsWith(this.DEFAULT_PREFIX) ? key : `${this.DEFAULT_PREFIX}${key}`;
  }
}