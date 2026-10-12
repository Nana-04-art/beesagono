import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { of } from 'rxjs';
import { GameApiService } from './game-api.service';
import { PuzzleService } from '../puzzle/puzzle.service';
import { GameBoard } from '../../models/game/game-board.model';
import { GameSessionResponse, SubmitWordResponse } from '../../models/game/game-dto.model';
import { environment } from '../../environments/environment';

describe('GameApiService', () => {
  let service: GameApiService;
  let httpTesting: HttpTestingController;

  let mockPuzzleService: {
    getTodayPuzzle: ReturnType<typeof vi.fn>;
  };

  const mockBoard: GameBoard = {
    date: '2026-10-12',
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

  const mockSession: GameSessionResponse = {
    id: 'session-123',
    puzzleId: 'puzzle-1',
    userId: 'user-456',
    currentScore: 5,
    currentRankLabel: 'Principiante',
    foundWords: ['MELE'],
    invalidWords: ['MEEE'],
    foundMielegrammi: [],
    isCompleted: false,
    startTime: new Date().toISOString(),
    lastUpdated: new Date().toISOString(),
  };

  beforeEach(() => {
    mockPuzzleService = {
      getTodayPuzzle: vi.fn().mockReturnValue(of(mockBoard)),
    };

    TestBed.configureTestingModule({
      providers: [
        GameApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PuzzleService, useValue: mockPuzzleService },
      ],
    });

    service = TestBed.inject(GameApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
    expect(service.loadStatus()).toBe('idle');
  });

  it('should load today session and board successfully', async () => {
    const loadPromise = service.loadTodaySession();

    const req = httpTesting.expectOne(`${environment.apiBaseUrl}/game/session/today`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSession);

    await loadPromise;

    expect(service.loadStatus()).toBe('ready');
    expect(service.session()).toEqual(mockSession);
    expect(service.board()).toEqual(mockBoard);
    expect(service.foundWords()).toEqual(['MELE']);
    expect(service.invalidWords()).toEqual(['MEEE']);
  });

  it('should set loadStatus to error if session loading fails', async () => {
    const loadPromise = service.loadTodaySession();

    const req = httpTesting.expectOne(`${environment.apiBaseUrl}/game/session/today`);
    req.error(new ProgressEvent('Network error'));

    await loadPromise;

    expect(service.loadStatus()).toBe('error');
    expect(service.session()).toBeNull();
  });

  it('should submit a valid word and update session state', async () => {
    // Carica la sessione
    const loadPromise = service.loadTodaySession();
    httpTesting.expectOne(`${environment.apiBaseUrl}/game/session/today`).flush(mockSession);
    await loadPromise;

    // Digita la parola
    'MIELE'.split('').forEach((char) => service.handleInput(char));
    expect(service.currentInput()).toBe('MIELE');

    const submitPromise = service.submitWord();

    const submitReq = httpTesting.expectOne(`${environment.apiBaseUrl}/game/submit`);
    expect(submitReq.request.method).toBe('POST');
    expect(submitReq.request.body).toEqual({ sessionId: 'session-123', word: 'MIELE' });

    const successResponse: SubmitWordResponse = {
      success: true,
      word: 'MIELE',
      pointsEarned: 5,
      currentScore: 10,
      currentRankLabel: 'Intermedio',
      isMielegramma: false,
    };

    submitReq.flush(successResponse);
    const result = await submitPromise;

    expect(result.isValid).toBe(true);
    expect(result.pointsAwarded).toBe(5);
    expect(service.currentInput()).toBe('');
    expect(service.foundWords()).toContain('MIELE');
    expect(service.session()?.currentScore).toBe(10);
  });

  it('should handle invalid word submission from backend response', async () => {
    const loadPromise = service.loadTodaySession();
    httpTesting.expectOne(`${environment.apiBaseUrl}/game/session/today`).flush(mockSession);
    await loadPromise;

    'MEEEE'.split('').forEach((char) => service.handleInput(char));

    const submitPromise = service.submitWord();

    const submitReq = httpTesting.expectOne(`${environment.apiBaseUrl}/game/submit`);

    const invalidResponse: SubmitWordResponse = {
      success: false,
      word: 'MEEEE',
      pointsEarned: 0,
      currentScore: 5,
      currentRankLabel: 'Principiante',
      isMielegramma: false,
      errorCode: 'NOT_IN_DICTIONARY',
      errorMessage: 'La parola non è nel dizionario',
    };

    submitReq.flush(invalidResponse);
    const result = await submitPromise;

    expect(result.isValid).toBe(false);
    expect(result.errorType).toBe('NOT_IN_DICTIONARY');
    expect(service.invalidWords()).toContain('MEEEE');
  });

  it('should handle server connection error gracefully during submit', async () => {
    const loadPromise = service.loadTodaySession();
    httpTesting.expectOne(`${environment.apiBaseUrl}/game/session/today`).flush(mockSession);
    await loadPromise;

    'MIELE'.split('').forEach((char) => service.handleInput(char));

    const submitPromise = service.submitWord();

    const submitReq = httpTesting.expectOne(`${environment.apiBaseUrl}/game/submit`);
    submitReq.error(new ProgressEvent('500 Error'));

    const result = await submitPromise;

    expect(result.isValid).toBe(false);
    expect(result.message).toBe('Server connection error');
    expect(service.currentInput()).toBe('');
  });

  it('should manipulate current input with deleteLastChar and clearInput', async () => {
    const loadPromise = service.loadTodaySession();
    httpTesting.expectOne(`${environment.apiBaseUrl}/game/session/today`).flush(mockSession);
    await loadPromise;

    service.handleInput('A');
    service.handleInput('B');
    expect(service.currentInput()).toBe('AB');

    service.deleteLastChar();
    expect(service.currentInput()).toBe('A');

    service.clearInput();
    expect(service.currentInput()).toBe('');
  });

  it('should reorder display cells on shuffle', async () => {
    const loadPromise = service.loadTodaySession();
    httpTesting.expectOne(`${environment.apiBaseUrl}/game/session/today`).flush(mockSession);
    await loadPromise;

    const originalCenter = service.displayCells()[0];
    service.shuffle();
    const newCenter = service.displayCells()[0];

    expect(newCenter.id).toBe(originalCenter.id);
    expect(newCenter.isCenter).toBe(true);
  });
});