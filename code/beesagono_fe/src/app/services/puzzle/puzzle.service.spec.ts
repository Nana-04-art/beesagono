import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { PuzzleService } from './puzzle.service';
import { DailyPuzzleResponse } from '../../models/puzzle/daily-puzzle-response.model';
import { GameBoard } from '../../models/game/game-board.model';
import { environment } from '../../environments/environment';
import { getTodayIsoString } from '../../utils/game-state.validator'; 

describe('PuzzleService', () => {
  let service: PuzzleService;
  let httpMock: HttpTestingController;

  const todayStr = getTodayIsoString(new Date());

  const mockPuzzleResponse: DailyPuzzleResponse = {
    id: 'puzzle-123',
    puzzleDate: todayStr,
    centerLetter: 'a',
    outerLetters: ['b', 'c', 'd', 'e', 'f', 'g'],
    maxScore: 100,
    totalWordsCount: 25,
    totalMielegrammiCount: 2,
    wordsCountByLetter: { A: 10, B: 5, C: 10 },
    mielegrammiCountByLetter: { A: 1, B: 1 },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PuzzleService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(PuzzleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that there are no outstanding HTTP requests after each test
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getTodayPuzzle', () => {
    it('should fetch today puzzle and correctly map DailyPuzzleResponse to GameBoard model', () => {
      let resultBoard: GameBoard | undefined;

      service.getTodayPuzzle().subscribe((board) => {
        resultBoard = board;
      });

      // Intercept the GET call
      const req = httpMock.expectOne(`${environment.apiBaseUrl}/puzzles/today`);
      expect(req.request.method).toBe('GET');

      // Respond with the mocked DTO
      req.flush(mockPuzzleResponse);

      // Verifications on the transformed GameBoard
      expect(resultBoard).toBeDefined();
      expect(resultBoard?.date).toBe(todayStr);
      expect(resultBoard?.seed).toBe('puzzle-123');
      expect(resultBoard?.maxScore).toBe(100);
      expect(resultBoard?.totalWordsCount).toBe(25);
      expect(resultBoard?.totalMielegrammiCount).toBe(2);
      expect(resultBoard?.wordsCountByLetter).toEqual({ A: 10, B: 5, C: 10 });
      expect(resultBoard?.mielegrammiCountByLetter).toEqual({ A: 1, B: 1 });

      // Verify cell mapping (7 total cells: 1 center + 6 outer)
      expect(resultBoard?.cells).toHaveLength(7);

      // Center cell (hex-0)
      const centerCell = resultBoard?.cells[0];
      expect(centerCell).toEqual({
        id: 'hex-0',
        letter: 'A', // Must be uppercase
        position: 0,
        isCenter: true,
      });

      // Outer cells (hex-1 to hex-6)
      const outerCells = resultBoard?.cells.slice(1);
      expect(outerCells).toHaveLength(6);

      outerCells?.forEach((cell, index) => {
        expect(cell).toEqual({
          id: `hex-${index + 1}`,
          letter: mockPuzzleResponse.outerLetters[index].toUpperCase(),
          position: index + 1,
          isCenter: false,
        });
      });
    });

    it('should fallback seed to puzzleDate if id is missing/falsy', () => {
      const responseWithoutId: DailyPuzzleResponse = {
        ...mockPuzzleResponse,
        id: '',
      };

      let resultBoard: GameBoard | undefined;

      service.getTodayPuzzle().subscribe((board) => {
        resultBoard = board;
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/puzzles/today`);
      req.flush(responseWithoutId);

      expect(resultBoard?.seed).toBe(todayStr);
    });

    it('should handle empty outerLetters, possibleWords, or mielegrammi gracefully', () => {
      const emptyResponse: DailyPuzzleResponse = {
        id: 'puzzle-456',
        puzzleDate: todayStr,
        centerLetter: 'x',
        outerLetters: [],
        maxScore: 0,
      };

      let resultBoard: GameBoard | undefined;

      service.getTodayPuzzle().subscribe((board) => {
        resultBoard = board;
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/puzzles/today`);
      req.flush(emptyResponse);

      expect(resultBoard?.cells).toHaveLength(1); // only the center cell
      expect(resultBoard?.cells[0].letter).toBe('X');
    });

    it('should handle HTTP error appropriately', () => {
      let actualError: any;

      service.getTodayPuzzle().subscribe({
        next: () => expect.fail('Should have failed with HTTP 500 error'),
        error: (err) => {
          actualError = err;
        },
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/puzzles/today`);
      req.flush('Internal Server Error', {
        status: 500,
        statusText: 'Server Error',
      });

      expect(actualError.status).toBe(500);
    });
  });
});