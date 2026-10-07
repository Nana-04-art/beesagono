import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, beforeEach, afterEach, it, expect } from 'vitest';
import { PuzzleService } from './puzzle.service';
import { DailyPuzzleResponse } from '../../models/puzzle/daily-puzzle-response.model';
import { GameBoard } from '../../models/game/game-board.model';
import { environment } from '../../environments/environment';

describe('PuzzleService', () => {
  let service: PuzzleService;
  let httpMock: HttpTestingController;

  const mockPuzzleResponse: DailyPuzzleResponse = {
    id: 'puzzle-123',
    puzzleDate: '2026-09-30',
    centerLetter: 'a',
    outerLetters: ['b', 'c', 'd', 'e', 'f', 'g'],
    validWords: ['ABBA', 'CABA', 'BABE'],
    mielegrammi: ['BABCADEG'],
    maxScore: 100,
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
    if (httpMock) {
      httpMock.verify();
    }
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

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/puzzles/today`);
      expect(req.request.method).toBe('GET');

      req.flush(mockPuzzleResponse);

      expect(resultBoard).toBeDefined();
      expect(resultBoard?.date).toBe('2026-09-30');
      expect(resultBoard?.seed).toBe('puzzle-123');
      expect(resultBoard?.maxScore).toBe(100);
      expect(resultBoard?.possibleWords).toEqual(['ABBA', 'CABA', 'BABE']);
      expect(resultBoard?.mielegrammi).toEqual(['BABCADEG']);

      expect(resultBoard?.cells).toHaveLength(7);

      const centerCell = resultBoard?.cells[0];
      expect(centerCell).toEqual({
        id: 'hex-0',
        letter: 'A',
        position: 0,
        isCenter: true,
      });

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

      expect(resultBoard?.seed).toBe('2026-09-30');
    });

    it('should handle empty outerLetters, validWords, or mielegrammi gracefully', () => {
      const emptyResponse: DailyPuzzleResponse = {
        id: 'puzzle-456',
        puzzleDate: '2026-09-30',
        centerLetter: 'x',
        outerLetters: [],
        validWords: [],
        mielegrammi: [],
        maxScore: 0,
      };

      let resultBoard: GameBoard | undefined;

      service.getTodayPuzzle().subscribe((board) => {
        resultBoard = board;
      });

      const req = httpMock.expectOne(`${environment.apiBaseUrl}/puzzles/today`);
      req.flush(emptyResponse);

      expect(resultBoard?.cells).toHaveLength(1);
      expect(resultBoard?.cells[0].letter).toBe('X');
      expect(resultBoard?.possibleWords).toEqual([]);
      expect(resultBoard?.mielegrammi).toEqual([]);
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