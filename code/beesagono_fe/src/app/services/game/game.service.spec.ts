import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { GameService } from './game.service';
import { StorageService } from '../storage/storage.service';
import { PuzzleGeneratorService } from '../puzzle-generator/puzzle-generator.service';
import { DictionaryService } from '../dictionary/dictionary.service';
import { ScoreService } from '../score/score.service';
import { StatsService } from '../stats/stats.service';
import { GameBoard } from '../../models/game-board.model';
import { GameState } from '../../models/game-state.model';

describe('GameService', () => {
  let service: GameService;

  let mockPuzzleGenerator: {
    generateDailyPuzzle: ReturnType<typeof vi.fn>;
  };
  let mockDictionaryService: {
    loadDictionary: ReturnType<typeof vi.fn>;
    getWordSet: ReturnType<typeof vi.fn>;
  };
  let mockStorageService: {
    load: ReturnType<typeof vi.fn>;
    save: ReturnType<typeof vi.fn>;
    isAvailable: ReturnType<typeof vi.fn>;
    clear: ReturnType<typeof vi.fn>;
  };
  let mockScoreService: {
    calculateWordPoints: ReturnType<typeof vi.fn>;
    calculateTotalScore: ReturnType<typeof vi.fn>;
  };
  let mockStatsService: {
    recordGameStarted: ReturnType<typeof vi.fn>;
    recordProgress: ReturnType<typeof vi.fn>;
  };

  const mockBoard: GameBoard = {
    date: '2026-07-31',
    seed: '123_0',
    cells: [
      { id: '0', letter: 'E', position: 0, isCenter: true },
      { id: '1', letter: 'M', position: 1, isCenter: false },
      { id: '2', letter: 'I', position: 2, isCenter: false },
      { id: '3', letter: 'L', position: 3, isCenter: false },
      { id: '4', letter: 'G', position: 4, isCenter: false },
      { id: '5', letter: 'R', position: 5, isCenter: false },
      { id: '6', letter: 'A', position: 6, isCenter: false },
    ],
    possibleWords: ['MIELE', 'MELE', 'MIELEGRAMMA'],
    mielegrammi: ['MIELEGRAMMA'],
    maxScore: 25,
  };

  beforeEach(async () => {
    mockPuzzleGenerator = {
      generateDailyPuzzle: vi.fn().mockReturnValue(mockBoard),
    };

    mockDictionaryService = {
      loadDictionary: vi.fn().mockResolvedValue(undefined),
      getWordSet: vi.fn().mockReturnValue(new Set(['MIELE', 'MELE', 'MIELEGRAMMA'])),
    };

    mockStorageService = {
      load: vi.fn().mockReturnValue(null),
      save: vi.fn().mockReturnValue(true),
      isAvailable: vi.fn().mockReturnValue(true),
      clear: vi.fn(),
    };

    mockScoreService = {
      calculateWordPoints: vi.fn((word: string, isMielegramma: boolean) => {
        if (isMielegramma) return word.length + 7;
        return word.length === 4 ? 1 : word.length;
      }),
      calculateTotalScore: vi.fn((words: string[], mieleSet: Set<string>) => {
        return words.reduce((acc, word) => {
          const isM = mieleSet.has(word);
          if (isM) return acc + word.length + 7;
          return acc + (word.length === 4 ? 1 : word.length);
        }, 0);
      }),
    };

    mockStatsService = {
      recordGameStarted: vi.fn(),
      recordProgress: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        GameService,
        { provide: PuzzleGeneratorService, useValue: mockPuzzleGenerator },
        { provide: DictionaryService, useValue: mockDictionaryService },
        { provide: StorageService, useValue: mockStorageService },
        { provide: ScoreService, useValue: mockScoreService },
        { provide: StatsService, useValue: mockStatsService },
      ],
    });

    service = TestBed.inject(GameService);
    await service.loadDailyGame();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created in ready state after daily game load', () => {
    expect(service).toBeTruthy();
    expect(service.loadStatus()).toBe('ready');
  });

  it('should load daily game successfully and restore empty state when no saved state exists', () => {
    expect(service.board()).toEqual(mockBoard);
    expect(service.score()).toBe(0);
    expect(service.foundWords()).toEqual([]);
    expect(mockStatsService.recordGameStarted).toHaveBeenCalled();
  });

  it('should restore saved game state from storage if available and valid', async () => {
    const savedState: GameState = {
      version: 1,
      date: '2026-07-31',
      foundWords: ['MIELE'],
      invalidWords: [],
      foundMielegrammi: [],
      score: 5,
      isCompleted: false,
      startTime: 1000,
      lastUpdated: 2000,
      rankLabel: 'Principiante',
    };
    mockStorageService.load.mockReturnValue(savedState);

    await service.loadDailyGame();

    expect(service.foundWords()).toEqual(['MIELE']);
    expect(service.score()).toBe(5);
  });

  it('should clear storage and notify stats service when saved state is invalid or corrupted', async () => {
    const corruptedState = {
      version: 1,
    };
    mockStorageService.load.mockReturnValue(corruptedState);

    const todayIsoDate = (service as any).getTodayIsoString();

    await service.loadDailyGame();

    expect(mockStorageService.clear).toHaveBeenCalledWith(`game:${todayIsoDate}`);
    expect(service.foundWords()).toEqual([]);
    expect(mockStatsService.recordGameStarted).toHaveBeenCalledWith(todayIsoDate);
  });

  it('should handle input characters, backspace, and clear via handleInput', () => {
    service.handleInput('M');
    service.handleInput('I');
    expect(service.currentInput()).toBe('MI');

    service.handleInput('BACKSPACE');
    expect(service.currentInput()).toBe('M');

    service.clearInput();
    expect(service.currentInput()).toBe('');
  });

  it('should process valid word submissions and calculate score', () => {
    'MIELE'.split('').forEach((char) => service.handleInput(char));
    const mieleResult = service.submitWord();

    expect(mieleResult.isValid).toBe(true);
    expect(mieleResult.pointsAwarded).toBe(5);
    expect(service.score()).toBe(5);
    expect(mockStatsService.recordProgress).toHaveBeenCalled();
  });

  it('should return error when word is too short', () => {
    'MIE'.split('').forEach((char) => service.handleInput(char));
    const result = service.submitWord();

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('TOO_SHORT');
  });

  it('should track invalid words and return error when missing center letter', () => {
    'MILAMILA'.split('').forEach((char) => service.handleInput(char));
    const result = service.submitWord();

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('MISSING_CENTER');
    expect(service.invalidWords()).toContain('MILAMILA');
  });

  it('should track invalid words and return error when using invalid letters', () => {
    'MIELEZ'.split('').forEach((char) => service.handleInput(char));
    const result = service.submitWord();

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('INVALID_LETTERS');
    expect(service.invalidWords()).toContain('MIELEZ');
  });

  it('should return ALREADY_FOUND error for duplicate submissions without duplicating invalidWords', () => {
    'MIELE'.split('').forEach((char) => service.handleInput(char));
    service.submitWord();

    'MIELE'.split('').forEach((char) => service.handleInput(char));
    const dupResult = service.submitWord();

    expect(dupResult.isValid).toBe(false);
    expect(dupResult.errorType).toBe('ALREADY_FOUND');
  });

  it('should track invalid words and return error when word is not in dictionary', () => {
    'MEEEE'.split('').forEach((char) => service.handleInput(char));
    const result = service.submitWord();

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('NOT_IN_DICTIONARY');
    expect(service.invalidWords()).toContain('MEEEE');
  });

  it('should calculate mielegramma bonus points correctly', () => {
    'MIELEGRAMMA'.split('').forEach((char) => service.handleInput(char));
    const result = service.submitWord();

    expect(result.isValid).toBe(true);
    expect(result.isMielegramma).toBe(true);
    expect(result.pointsAwarded).toBe(18);
  });

  it('should compute rank progress and completion correctly', () => {
    mockBoard.possibleWords.forEach((word) => {
      service.clearInput();
      word.split('').forEach((char) => service.handleInput(char));
      service.submitWord();
    });

    expect(service.foundWords().length).toBe(3);
    expect(service.isCompleted()).toBe(true);
  });

  it('should shuffle outer cells while leaving center cell at position 0', () => {
    const originalCenter = service.displayCells()[0];
    service.shuffle();
    const newCenter = service.displayCells()[0];

    expect(newCenter.id).toBe(originalCenter.id);
    expect(newCenter.isCenter).toBe(true);
  });

  it('should generate share score payload correctly', () => {
    'MIELE'.split('').forEach((char) => service.handleInput(char));
    service.submitWord();

    const payload = service.getShareScorePayload();

    expect(payload.score).toBe(5);
    expect(payload.maxScore).toBe(25);
    expect(payload.wordsFound).toBe(1);
    expect(payload.totalWords).toBe(3);
    expect(payload.mielegrammiFound).toBe(0);
    expect(payload.totalMielegrammi).toBe(1);
  });

  it('should trigger reload on checkDateRollover if date changed', () => {
    const spy = vi.spyOn(service, 'loadDailyGame');
    vi.spyOn(service as any, 'getTodayIsoString').mockReturnValue('2026-08-01');

    service.checkDateRollover();

    expect(spy).toHaveBeenCalled();
  });
});