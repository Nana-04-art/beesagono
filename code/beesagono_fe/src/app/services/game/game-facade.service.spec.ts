import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { GameFacadeService } from './game-facade.service';
import { AuthService } from '../auth/auth.service';
import { GameService } from './game.service';
import { GameApiService } from './game-api.service';
import { ScoreService } from '../score/score.service';
import { GameBoard } from '../../models/game/game-board.model';
import { RankTier } from '../../models/game/rank.model';
import { ValidationResult } from '../../models/game/validation.model';

describe('GameFacadeService', () => {
  let service: GameFacadeService;

  // Signal per simulare lo stato di autenticazione
  const isLoggedInSignal = signal<boolean>(false);

  // Mock Boards
  const mockBoard: GameBoard = {
    date: '2026-10-12',
    seed: 'seed_1',
    cells: [],
    possibleWords: ['CASA', 'ALBERO', 'MIELEGRAMMA'],
    mielegrammi: ['MIELEGRAMMA'],
    maxScore: 30,
  };

  // Mock Services
  let mockAuthService: { isLoggedIn: typeof isLoggedInSignal };
  let mockGuestGame: Record<string, any>;
  let mockApiGame: Record<string, any>;
  let mockScoreService: { getCurrentRank: ReturnType<typeof vi.fn> };

  const defaultRank: RankTier = {
    threshold: 0,
    label: 'Principiante',
  };

  beforeEach(() => {
    isLoggedInSignal.set(false);

    mockAuthService = {
      isLoggedIn: isLoggedInSignal,
    };

    mockGuestGame = {
      board: signal<GameBoard | null>(mockBoard),
      loadStatus: signal<'idle' | 'loading' | 'ready' | 'error'>('ready'),
      loadError: signal<string | null>(null),
      currentInput: signal<string>('CAS'),
      foundWords: signal<string[]>(['CASA']),
      invalidWords: signal<string[]>(['X']),
      wordMap: signal([]),
      letterColors: signal(new Map()),
      displayCells: signal([]),
      score: signal<number>(5),
      loadDailyGame: vi.fn().mockResolvedValue(undefined),
      checkDateRollover: vi.fn(),
      submitWord: vi.fn().mockResolvedValue({ isValid: true, pointsAwarded: 5, isMielegramma: false }),
      handleInput: vi.fn().mockResolvedValue(undefined),
      deleteLastChar: vi.fn(),
      clearInput: vi.fn(),
      shuffle: vi.fn(),
    };

    mockApiGame = {
      board: signal<GameBoard | null>(mockBoard),
      loadStatus: signal<'idle' | 'loading' | 'ready' | 'error'>('ready'),
      session: signal({ currentScore: 20 }),
      currentInput: signal<string>('ALB'),
      foundWords: signal<string[]>(['CASA', 'ALBERO']),
      invalidWords: signal<string[]>([]),
      wordMap: signal([]),
      letterColors: signal(new Map()),
      displayCells: signal([]),
      loadTodaySession: vi.fn().mockResolvedValue(undefined),
      submitWord: vi.fn().mockResolvedValue({ isValid: true, pointsAwarded: 10, isMielegramma: false }),
      handleInput: vi.fn().mockResolvedValue(undefined),
      deleteLastChar: vi.fn(),
      clearInput: vi.fn(),
      shuffle: vi.fn(),
    };

    mockScoreService = {
      getCurrentRank: vi.fn().mockReturnValue(defaultRank),
    };

    TestBed.configureTestingModule({
      providers: [
        GameFacadeService,
        { provide: AuthService, useValue: mockAuthService },
        { provide: GameService, useValue: mockGuestGame },
        { provide: GameApiService, useValue: mockApiGame },
        { provide: ScoreService, useValue: mockScoreService },
      ],
    });

    service = TestBed.inject(GameFacadeService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Guest Mode State & Actions', () => {
    it('should route state signals to GuestGame when not logged in', () => {
      expect(service.isLoggedIn()).toBe(false);
      expect(service.board()).toEqual(mockBoard);
      expect(service.currentInput()).toBe('CAS');
      expect(service.foundWords()).toEqual(['CASA']);
      expect(service.score()).toBe(5);
    });

    it('should delegate loadDailyGame to GuestGame', async () => {
      await service.loadDailyGame();
      expect(mockGuestGame['loadDailyGame']).toHaveBeenCalledTimes(1);
      expect(mockApiGame['loadTodaySession']).not.toHaveBeenCalled();
    });

    it('should delegate submitWord to GuestGame', async () => {
      const result = await service.submitWord();
      expect(mockGuestGame['submitWord']).toHaveBeenCalledTimes(1);
      expect(result.isValid).toBe(true);
    });

    it('should delegate handleInput, deleteLastChar, clearInput, and shuffle to GuestGame', async () => {
      await service.handleInput('A');
      expect(mockGuestGame['handleInput']).toHaveBeenCalledWith('A');

      service.deleteLastChar();
      expect(mockGuestGame['deleteLastChar']).toHaveBeenCalledTimes(1);

      service.clearInput();
      expect(mockGuestGame['clearInput']).toHaveBeenCalledTimes(1);

      service.shuffle();
      expect(mockGuestGame['shuffle']).toHaveBeenCalledTimes(1);
    });

    it('should check date rollover only in Guest Mode', () => {
      service.checkDateRollover();
      expect(mockGuestGame['checkDateRollover']).toHaveBeenCalledTimes(1);
    });
  });

  describe('Logged-In User Mode State & Actions', () => {
    beforeEach(() => {
      isLoggedInSignal.set(true);
    });

    it('should route state signals to ApiGame when logged in', () => {
      expect(service.isLoggedIn()).toBe(true);
      expect(service.currentInput()).toBe('ALB');
      expect(service.foundWords()).toEqual(['CASA', 'ALBERO']);
      expect(service.score()).toBe(20);
    });

    it('should return server error string when loadStatus is error', () => {
      mockApiGame['loadStatus'].set('error');
      expect(service.loadError()).toBe('Errore nel caricamento della sessione dal server.');
    });

    it('should delegate loadDailyGame to ApiGame', async () => {
      await service.loadDailyGame();
      expect(mockApiGame['loadTodaySession']).toHaveBeenCalledTimes(1);
      expect(mockGuestGame['loadDailyGame']).not.toHaveBeenCalled();
    });

    it('should delegate submitWord to ApiGame', async () => {
      const result: ValidationResult = await service.submitWord();
      expect(mockApiGame['submitWord']).toHaveBeenCalledTimes(1);
      expect(result.pointsAwarded).toBe(10);
    });

    it('should delegate handleInput, deleteLastChar, clearInput, and shuffle to ApiGame', async () => {
      await service.handleInput('B');
      expect(mockApiGame['handleInput']).toHaveBeenCalledWith('B');

      service.deleteLastChar();
      expect(mockApiGame['deleteLastChar']).toHaveBeenCalledTimes(1);

      service.clearInput();
      expect(mockApiGame['clearInput']).toHaveBeenCalledTimes(1);

      service.shuffle();
      expect(mockApiGame['shuffle']).toHaveBeenCalledTimes(1);
    });

    it('should ignore checkDateRollover when logged in', () => {
      service.checkDateRollover();
      expect(mockGuestGame['checkDateRollover']).not.toHaveBeenCalled();
    });
  });

  describe('Computed Values & Payload Generation', () => {
    it('should calculate rank using ScoreService', () => {
      const rank = service.rank();
      expect(mockScoreService.getCurrentRank).toHaveBeenCalledWith(5, 30);
      expect(rank).toEqual(defaultRank);
    });

    it('should compute totalPossibleWords and totalMielegrammi', () => {
      expect(service.totalPossibleWords()).toBe(3);
      expect(service.totalMielegrammi()).toBe(1);
    });

    it('should compute foundMielegrammi correctly', () => {
      mockGuestGame['foundWords'].set(['CASA', 'MIELEGRAMMA']);
      expect(service.foundMielegrammi()).toEqual(['MIELEGRAMMA']);
    });

    it('should compute isCompleted when all words are found', () => {
      expect(service.isCompleted()).toBe(false);

      mockGuestGame['foundWords'].set(['CASA', 'ALBERO', 'MIELEGRAMMA']);
      expect(service.isCompleted()).toBe(true);
    });

    it('should construct ShareScorePayload correctly', () => {
      mockGuestGame['foundWords'].set(['CASA', 'MIELEGRAMMA']);

      const payload = service.getShareScorePayload();

      expect(payload).toEqual({
        date: '2026-10-12',
        score: 5,
        maxScore: 30,
        wordsFound: 2,
        totalWords: 3,
        mielegrammiFound: 1,
        totalMielegrammi: 1,
      });
    });
  });
});