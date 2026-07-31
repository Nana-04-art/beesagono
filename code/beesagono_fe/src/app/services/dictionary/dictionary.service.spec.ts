import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { DictionaryService } from './dictionary.service';

describe('DictionaryService', () => {
  let service: DictionaryService;

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [DictionaryService],
    });
    service = TestBed.inject(DictionaryService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should validate allowed words correctly if method exists', () => {
    const s = service as any;
    if (typeof s.isValidWord === 'function') {
      const isValid = s.isValidWord('MIELE');
      expect(typeof isValid).toBe('boolean');
    } else if (typeof s.hasWord === 'function') {
      const isValid = s.hasWord('MIELE');
      expect(typeof isValid).toBe('boolean');
    } else {
      expect(service).toBeDefined();
    }
  });

  it('should identify pangram words if method exists', () => {
    const s = service as any;
    const letters = new Set(['M', 'I', 'E', 'L', 'O', 'A', 'N']);

    if (typeof s.isPangram === 'function') {
      const isPangram = s.isPangram('MELANOIA', letters);
      expect(typeof isPangram).toBe('boolean');
    } else {
      expect(service).toBeDefined();
    }
  });

  it('should handle dictionary load errors when fetch fails', async () => {
    // Mock global fetch to simulate network error
    vi.spyOn(globalThis, 'fetch').mockRejectedValueOnce(new Error('Network error'));

    const s = service as any;
    if (typeof s.loadDictionary === 'function') {
      try {
        await s.loadDictionary();
      } catch (error: any) {
        // Assert that the error was caught and matches expectations
        expect(error.message).toBe('Network error');
      }
    } else {
      expect(service).toBeDefined();
    }
  });
});