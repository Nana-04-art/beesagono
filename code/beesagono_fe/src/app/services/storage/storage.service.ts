import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly DEFAULT_PREFIX = 'beesagono:';
  // In-memory fallback store for cases where localStorage is blocked or full
  private readonly inMemoryFallback = new Map<string, string>();
  private usingFallback = false;

  // Persists generic data to localStorage or falls back to in-memory store.
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

  // Reads and parses data from in-memory fallback or localStorage.
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

  // Removes a stored entry for a given key.
  clear(key: string): void {
    const fullKey = this.getFullKey(key);
    try {
      localStorage.removeItem(fullKey);
    } catch {
      // Ignored
    }
    this.inMemoryFallback.delete(fullKey);
  }

  // Returns true if running natively with localStorage.
  isAvailable(): boolean {
    return !this.usingFallback;
  }

  /**
   * Retrieves all keys starting with a given prefix,
   * unifying localStorage and in-memory fallback sources.
   */
  getKeysByPrefix(prefix: string): string[] {
    const fullPrefix = this.getFullKey(prefix);
    const keys = new Set<string>();

    // Search in localStorage if available
    try {
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith(fullPrefix)) {
          keys.add(key);
        }
      }
    } catch (e) {
      console.warn('[StorageService] localStorage enumeration failed or unavailable:', e);
    }

    // Merge keys saved in the in-memory fallback
    for (const key of this.inMemoryFallback.keys()) {
      if (key.startsWith(fullPrefix)) {
        keys.add(key);
      }
    }

    return Array.from(keys);
  }

  private getFullKey(key: string): string {
    return key.startsWith(this.DEFAULT_PREFIX) ? key : `${this.DEFAULT_PREFIX}${key}`;
  }
}