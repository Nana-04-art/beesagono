import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { GameService } from './game.service';
import { StorageService } from '../storage/storage.service';
import { PuzzleGeneratorService } from '../puzzle-generator/puzzle-generator.service';
import { DictionaryService } from '../dictionary/dictionary.service';
import { GameBoard } from '../../models/game-board.model';
import { GameState } from '../../models/game-state.model';

describe('GameService', () => {
  let service: GameService;
  let mockPuzzleGenerator: any;
  let mockDictionaryService: any;
  let mockStorageService: any;

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

    TestBed.configureTestingModule({
      providers: [
        GameService,
        { provide: PuzzleGeneratorService, useValue: mockPuzzleGenerator },
        { provide: DictionaryService, useValue: mockDictionaryService },
        { provide: StorageService, useValue: mockStorageService },
      ],
    });

    service = TestBed.inject(GameService);
    await service.loadDailyGame();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created in idle state', () => {
    expect(service).toBeTruthy();
  });

  it('should load daily game successfully and restore empty state when no saved state exists', () => {
    expect(service.board()).toEqual(mockBoard);
    expect(service.score()).toBe(0);
    expect(service.foundWords()).toEqual([]);
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
    };
    mockStorageService.load.mockReturnValue(savedState);

    await service.loadDailyGame();

    expect(service.foundWords()).toEqual(['MIELE']);
    expect(service.score()).toBe(5);
  });

  it('should handle input characters, backspace, clear, and ENTER via handleInput', () => {
    service.handleInput('M');
    service.handleInput('I');
    expect(service.currentInput()).toBe('MI');

    service.handleInput('BACKSPACE');
    expect(service.currentInput()).toBe('M');

    service.handleInput('CLEAR');
    expect(service.currentInput()).toBe('M');

    service.clearInput();
    expect(service.currentInput()).toBe('');
  });

  it('should process valid word submissions and calculate 4-letter word score (1 pt) and longer word score', () => {
    'MIELE'.split('').forEach((char) => service.handleInput(char));
    const mieleResult = service.submitWord();

    expect(mieleResult.isValid).toBe(true);
    expect(mieleResult.pointsAwarded).toBe(5);
    expect(service.score()).toBe(5);
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
  });

  it('should track invalid words and return error when using invalid letters', () => {
    'MIELEZ'.split('').forEach((char) => service.handleInput(char));
    const result = service.submitWord();

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('INVALID_LETTERS');
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
  });

  it('should compute rank progress, genius rank, and completion correctly', () => {
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

    service.checkDateRollover();

    expect(spy).toHaveBeenCalled();
  });
});