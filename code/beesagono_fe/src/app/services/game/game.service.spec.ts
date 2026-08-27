import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { GameService, getTodayIsoString } from './game.service';
import { StorageService } from '../storage/storage.service';
import { PuzzleGeneratorService } from '../puzzle-generator/puzzle-generator.service';
import { DictionaryService } from '../dictionary/dictionary.service';
import { ScoreService } from '../score/score.service';
import { StatsService } from '../stats/stats.service';
import { GameBoard } from '../../models/game-board.model';
import { GameState } from '../../models/game-state.model';
import { RANK_TIERS } from '../../config/rank-tiers.config';

describe('GameService', () => {
  let service: GameService;

  const todayStr = getTodayIsoString();

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
    getCurrentRank: ReturnType<typeof vi.fn>;
  };
  let mockStatsService: {
    recordGameStarted: ReturnType<typeof vi.fn>;
    recordProgress: ReturnType<typeof vi.fn>;
  };

  const mockBoard: GameBoard = {
    date: todayStr,
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
    maxScore: 24,
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
      calculateTotalScore: vi.fn((words: string[], mieleSet: Set<string>) => {
        let score = 0;
        if (words.includes('MELE')) score += 1;
        if (words.includes('MIELE')) score += 5;
        if (words.includes('MIELEGRAMMA')) score += 18; // 11 letters + 7 honeygram bonuses = 18
        return score;
      }),
      calculateWordPoints: vi.fn((word: string, isMielegramma: boolean) => {
        if (word === 'MELE') return 1;
        if (word === 'MIELE') return 5;
        if (isMielegramma) return 18; // 11 letters + 7 honeygram bonuses = 18
        return 0;
      }),
      getCurrentRank: vi.fn((score: number, maxScore: number) => {
        if (maxScore === 0) return RANK_TIERS[0];
        const percentage = (score / maxScore) * 100;
        const reversed = [...RANK_TIERS].reverse();
        return reversed.find((t) => percentage >= t.threshold) ?? RANK_TIERS[0];
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

  it('should be created and initialized', () => {
    expect(service).toBeTruthy();
    expect(service.loadStatus()).toBe('ready');
  });

  it('should load daily game successfully and notify StatsService when no state exists', () => {
    expect(service.board()).toEqual(mockBoard);
    expect(service.score()).toBe(0);
    expect(service.foundWords()).toEqual([]);
    expect(mockStatsService.recordGameStarted).toHaveBeenCalledWith(todayStr);
  });

  it('should restore saved game state from storage if available and valid', async () => {
    const savedState: GameState = {
      version: 1,
      date: todayStr,
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

  it('should clear storage and reset state if saved state is corrupt or incomplete', async () => {
    mockStorageService.load.mockReturnValue({ version: 1 });

    await service.loadDailyGame();

    expect(mockStorageService.clear).toHaveBeenCalledWith(`game:${todayStr}`);
    expect(service.foundWords()).toEqual([]);
    expect(service.invalidWords()).toEqual([]);
    expect(mockStatsService.recordGameStarted).toHaveBeenCalledWith(todayStr);
  });

  it('should handle input characters, backspace, and ENTER via handleInput', () => {
    service.handleInput('M');
    service.handleInput('I');
    expect(service.currentInput()).toBe('MI');

    service.handleInput('BACKSPACE');
    expect(service.currentInput()).toBe('M');

    service.clearInput();
    expect(service.currentInput()).toBe('');
  });

  it('should process valid word submissions and record progress in StatsService', () => {
    'MIELE'.split('').forEach((char) => service.handleInput(char));
    const mieleResult = service.submitWord();

    expect(mieleResult.isValid).toBe(true);
    expect(mieleResult.pointsAwarded).toBe(5);
    expect(service.score()).toBe(5);
    expect(mockStatsService.recordProgress).toHaveBeenCalledWith(todayStr, 5, false, expect.any(String));
  });

  it('should not add empty strings to invalidWords when submitting empty input', () => {
    service.clearInput();
    const result = service.submitWord();

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('TOO_SHORT');
    expect(service.invalidWords()).toEqual([]);
  });

  it('should return error when word is too short', () => {
    'MIE'.split('').forEach((char) => service.handleInput(char));
    const result = service.submitWord();

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('TOO_SHORT');
    expect(service.invalidWords()).toContain('MIE');
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
    expect(service.score()).toBe(18);
    expect(service.foundMielegrammi()).toContain('MIELEGRAMMA');
  });

  it('should compute completion correctly when all words are found', () => {
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

  it('should generate wordMap ordered by length and alphabet', () => {
    'MELE'.split('').forEach((char) => service.handleInput(char));
    service.submitWord();

    const map = service.wordMap();
    expect(map.length).toBe(3);
    expect(map[0].word).toBe('MELE');
    expect(map[0].isFound).toBe(true);
    expect(map[2].word).toBe('MIELEGRAMMA');
    expect(map[2].isPangram).toBe(true);
  });

  it('should map letterColors correctly based on board cells', () => {
    const colors = service.letterColors();
    expect(colors.size).toBe(7);
    expect(colors.has('E')).toBe(true);
    expect(colors.has('M')).toBe(true);
  });

  it('should generate share score payload correctly', () => {
    'MIELE'.split('').forEach((char) => service.handleInput(char));
    service.submitWord();

    const payload = service.getShareScorePayload();

    expect(payload.score).toBe(5);
    expect(payload.maxScore).toBe(24);
    expect(payload.wordsFound).toBe(1);
    expect(payload.totalWords).toBe(3);
    expect(payload.mielegrammiFound).toBe(0);
    expect(payload.totalMielegrammi).toBe(1);
  });

  it('should trigger reload on checkDateRollover if date changed', () => {
    const spy = vi.spyOn(service, 'loadDailyGame');
    vi.spyOn(service as any, 'getTodayIsoString').mockReturnValue('2026-08-01');

    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    vi.setSystemTime(tomorrow);

    service.checkDateRollover();

    expect(spy).toHaveBeenCalledTimes(1);

    vi.useRealTimers();
  });
});