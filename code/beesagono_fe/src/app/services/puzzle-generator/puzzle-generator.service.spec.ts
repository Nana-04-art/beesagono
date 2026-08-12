import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { PuzzleGeneratorService } from './puzzle-generator.service';
import { DictionaryService } from '../dictionary/dictionary.service';
import { GAME_RULES } from '../../config/game-rules.config';

describe('PuzzleGeneratorService', () => {
  let service: PuzzleGeneratorService;

  const mockWordsArray = ['MELANOIA', 'CAMICIA', 'MIELE', 'CASALE', 'LIME'];
  const mockPangramsArray = ['MELANOIA'];
  const mockSet = new Set(mockWordsArray);

  const mockDictionaryService = {
    loadDictionary: vi.fn().mockResolvedValue(mockWordsArray),
    getWordSet: vi.fn().mockReturnValue(mockSet),
  };

  beforeEach(() => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        PuzzleGeneratorService,
        { provide: DictionaryService, useValue: mockDictionaryService },
      ],
    });

    service = TestBed.inject(PuzzleGeneratorService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should generate a puzzle structure with exactly 7 cells and 7 unique letters', () => {
    const puzzle = service.generateDailyPuzzle('2026-07-31', mockSet);

    expect(puzzle).toBeDefined();
    expect(puzzle.cells.length).toBe(GAME_RULES.REQUIRED_LETTERS_COUNT);

    const extractedLetters = puzzle.cells.map((cell) => cell.letter);
    const uniqueLetters = new Set(extractedLetters);

    expect(extractedLetters.length).toBe(7);
    expect(uniqueLetters.size).toBe(7);
  });

  it('should ensure the center letter is present and configured at position 0', () => {
    const puzzle = service.generateDailyPuzzle('2026-07-31', mockSet);

    const centerCell = puzzle.cells.find((cell) => cell.isCenter);
    expect(centerCell).toBeDefined();
    expect(centerCell?.position).toBe(0);

    const allLetters = puzzle.cells.map((c) => c.letter);
    expect(allLetters).toContain(centerCell!.letter);
  });

  it('should produce deterministic puzzle results for the same date input', () => {
    const puzzle1 = service.generateDailyPuzzle('2026-07-31', mockSet);
    const puzzle2 = service.generateDailyPuzzle('2026-07-31', mockSet);

    expect(puzzle1).toEqual(puzzle2);
    expect(puzzle1.seed).toBe(puzzle2.seed);
  });

  it('should strictly maintain 7 cells even when fallback candidates are used (empty dictionary)', () => {
    const emptySet = new Set<string>();
    const fallbackPuzzle = service.generateDailyPuzzle('2026-07-31', emptySet);

    expect(fallbackPuzzle.cells.length).toBe(7);
    const uniqueLetters = new Set(fallbackPuzzle.cells.map((c) => c.letter));
    expect(uniqueLetters.size).toBe(7);
  });
});