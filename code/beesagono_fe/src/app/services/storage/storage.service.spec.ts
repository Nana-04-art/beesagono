import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { StorageService } from './storage.service';
import { GameState } from '../../models/game-state.model';

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
    // Reset TestBed to allow reconfiguration in Vitest environment
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

  it('should prioritize in-memory fallback when localStorage save fails', () => {
    // Persist State A successfully
    service.save(mockStateA);

    // Mock localStorage.setItem to throw an error using Vitest vi.spyOn
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });

    // Save State B (must go to inMemoryFallback)
    service.save(mockStateB);

    // Load state - must return State B, not State A from localStorage
    const loaded = service.load(mockStateA.date);
    expect(loaded?.foundWords).toEqual(['A', 'B']);
    expect(loaded?.score).toBe(2);
  });

  it('should read from inMemoryFallback if localStorage.getItem throws', () => {
    service.save(mockStateA);

    // Mock localStorage.getItem to throw using vi.spyOn
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('AccessDenied');
    });

    // Manually ensure entry in fallback map
    (service as any).inMemoryFallback.set('beesagono:game:2026-07-31', JSON.stringify(mockStateA));

    const loaded = service.load('2026-07-31');
    expect(loaded).toEqual(mockStateA);
  });
});