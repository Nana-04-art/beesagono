import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { StorageService } from './storage.service';
import { GameState } from '../../models/game-state.model';

export class MockThemeService {
  readonly currentTheme = signal<'light' | 'dark'>('light');
  isDarkMode = vi.fn().mockReturnValue(false);
  toggleTheme = vi.fn();
}

describe('StorageService', () => {
  let service: StorageService;

  const mockStateA: GameState = {
    version: 1,
    date: '2026-07-31',
    foundWords: ['A'],
    invalidWords: [],
    score: 1,
    foundMielegrammi: [],
    isCompleted: false,
    startTime: 1000,
    lastUpdated: 1000,
  };

  const mockStateB: GameState = {
    ...mockStateA,
    foundWords: ['A', 'B'],
    invalidWords: [],
    score: 2,
    lastUpdated: 2000,
  };

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [StorageService],
    });

    service = TestBed.inject(StorageService);
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should successfully save and load valid data using prefixed keys', () => {
    const success = service.save('test-key', mockStateA);
    expect(success).toBe(true);
    expect(service.isAvailable()).toBe(true);

    const loaded = service.load<GameState>('test-key');
    expect(loaded).toEqual(mockStateA);
  });

  it('should return false and warn when saving invalid key or undefined/null data', () => {
    const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => { });

    expect(service.save('', mockStateA)).toBe(false);
    expect(service.save('key', undefined)).toBe(false);
    expect(service.save('key', null)).toBe(false);

    expect(consoleWarnSpy).toHaveBeenCalled();
  });

  it('should return null when loading non-existent or empty key', () => {
    expect(service.load('')).toBeNull();
    expect(service.load('non-existent')).toBeNull();
  });

  it('should handle JSON parse errors gracefully and return null', () => {
    localStorage.setItem('beesagono:corrupted', 'invalid-json-content');
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => { });

    const loaded = service.load('corrupted');
    expect(loaded).toBeNull();
    expect(consoleErrorSpy).toHaveBeenCalled();
  });

  it('should clear stored items correctly from both localStorage and inMemoryFallback', () => {
    service.save('temp-key', mockStateA);
    expect(service.load('temp-key')).not.toBeNull();

    service.clear('temp-key');
    expect(service.load('temp-key')).toBeNull();
  });

  it('should handle errors thrown by removeItem gracefully in clear()', () => {
    const fullKey = 'beesagono:temp-key';
    (service as unknown as { inMemoryFallback: Map<string, string> }).inMemoryFallback.set(
      fullKey,
      JSON.stringify(mockStateA)
    );

    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
      throw new Error('Access Denied');
    });

    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => null);

    expect(() => service.clear('temp-key')).not.toThrow();
    expect(service.load('temp-key')).toBeNull();
  });

  it('should prioritize in-memory fallback when localStorage save fails', () => {
    service.save('2026-07-31', mockStateA);

      const gameKeys = service.getKeysByPrefix('game:');
      expect(gameKeys).toContain('beesagono:game:2026-08-01');
      expect(gameKeys).toContain('beesagono:game:2026-08-02');
      expect(gameKeys).not.toContain('beesagono:stats:user');
    });

    it('should return empty array if no keys match the prefix', () => {
      service.save('other:key', mockStateA);
      const keys = service.getKeysByPrefix('game:');
      expect(keys).toEqual([]);
    });
  });

  describe('Fallback mechanisms', () => {
    it('should prioritize in-memory fallback when localStorage save fails', () => {
      service.save('2026-07-31', mockStateA);

      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('QuotaExceededError');
      });

      const success = service.save('2026-07-31', mockStateB);
      expect(success).toBe(false);
      expect(service.isAvailable()).toBe(false);

      const loaded = service.load<GameState>('2026-07-31');
      expect(loaded?.foundWords).toEqual(['A', 'B']);
      expect(loaded?.score).toBe(2);
    });

    it('should read from inMemoryFallback if localStorage value is missing but fallback has it', () => {
      (service as any).inMemoryFallback.set('beesagono:fallback-key', JSON.stringify(mockStateA));

      const loaded = service.load<GameState>('fallback-key');
      expect(loaded).toEqual(mockStateA);
    });
  });

  describe('getKeysByPrefix', () => {
    it('should retrieve and unify keys matching prefix from both localStorage and fallback', () => {
      service.save('game:2026-07-30', mockStateA);
      service.save('game:2026-07-31', mockStateB);
      service.save('stats:overview', { streak: 5 });

      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('QuotaExceededError');
      });
      service.save('game:2026-08-01', mockStateA);

      const gameKeys = service.getKeysByPrefix('game:');

      expect(gameKeys).toContain('beesagono:game:2026-07-30');
      expect(gameKeys).toContain('beesagono:game:2026-07-31');
      expect(gameKeys).toContain('beesagono:game:2026-08-01');
      expect(gameKeys).not.toContain('beesagono:stats:overview');
      expect(gameKeys.length).toBe(3);
    });

    it('should handle enumeration failure in localStorage gracefully and return keys from fallback', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => { });

      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('QuotaExceededError');
      });
      service.save('game:2026-08-01', mockStateA);

      vi.spyOn(Storage.prototype, 'key').mockImplementation(() => {
        throw new Error('SecurityError: localStorage blocked');
      });

      const keys = service.getKeysByPrefix('game:');

      expect(keys).toEqual(['beesagono:game:2026-08-01']);
      expect(consoleWarnSpy).toHaveBeenCalled();
    });
  });
});