import { Injectable, Signal, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';
import { GameSessionResponse, SubmitWordRequest, SubmitWordResponse } from '../../models/game/game-dto.model';
import { ValidationErrorType, ValidationResult } from '../../models/game/validation.model';
import { WordMapItem } from '../../models/game/word-map-item.model';
import { GameBoard } from '../../models/game/game-board.model';
import { PuzzleService } from '../puzzle/puzzle.service';
import { buildLetterColorsMap, buildWordMapForLetter } from '../../utils/word-map.utils';
import { processInput, deleteLastChar, clearInput, shuffleOrder } from '../../utils/game-input.utils';
import { environment } from '../../environments/environment';
import { Cell } from '../../models/game/cell.model';

@Injectable({
  providedIn: 'root'
})
/** Service for the game logic aligned with GameServiceImpl Spring Boot Backend (Logged-in users) */
export class GameApiService {
  private readonly http = inject(HttpClient);
  private readonly puzzleService = inject(PuzzleService);
  private readonly baseUrl = `${environment.apiBaseUrl}/game`;

  // Remote State (Logged-in User)
  private readonly _session = signal<GameSessionResponse | null>(null);
  private readonly _board = signal<GameBoard | null>(null);
  private readonly _loadStatus = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');
  private readonly _currentInput = signal<string>('');
  readonly _shuffleOrder = signal<number[]>([1, 2, 3, 4, 5, 6]);

  readonly session = this._session.asReadonly();
  readonly board = this._board.asReadonly();
  readonly loadStatus = this._loadStatus.asReadonly();
  readonly currentInput = this._currentInput.asReadonly();

  // Convert Set/Array safely from backend DTO
  readonly foundWords = computed(() => Array.from(this._session()?.foundWords ?? []));
  readonly invalidWords = computed(() => Array.from(this._session()?.invalidWords ?? []));

  readonly wordsCountByLetter = computed(() => this._board()?.wordsCountByLetter ?? {});

  // Identificazione dei mielegrammi trovati basata sulla regola delle 7 lettere uniche
  readonly foundMielegrammi = computed(() => {
    return this.foundWords().filter((word) => new Set(word.toUpperCase()).size === 7);
  });

  readonly letterColors = computed(() => buildLetterColorsMap(this._board()));

  readonly wordMap: Signal<WordMapItem[]> = computed(() => {
    const counts = this.wordsCountByLetter();
    const found = this.foundWords();
    const colors = this.letterColors();

    // Raggruppa le parole trovate per lettera iniziale
    const foundByLetter: Record<string, string[]> = {};
    for (const word of found) {
      const initial = word.charAt(0).toUpperCase();
      if (!foundByLetter[initial]) {
        foundByLetter[initial] = [];
      }
      foundByLetter[initial].push(word);
    }

    let allItems: WordMapItem[] = [];
    const letters = Object.keys(counts).sort();

    for (const letter of letters) {
      const totalCount = counts[letter] ?? 0;
      const foundForLetter = foundByLetter[letter] ?? [];
      
      // Utilizzo diretto del metodo esistente nelle utility
      const letterItems = buildWordMapForLetter(letter, totalCount, foundForLetter);

      const color = colors.get(letter);
      if (color) {
        letterItems.forEach(item => item.color = color);
      }

      allItems = allItems.concat(letterItems);
    }

    return allItems;
  });

  // --- HTTP Backend Calls ---

  // --- GET TODAY SESSION ---
  getTodaySessionApi(): Observable<GameSessionResponse> {
    return this.http.get<GameSessionResponse>(`${this.baseUrl}/session/today`);
  }
  // --- SUBMIT WORD ---
  submitWordApi(request: SubmitWordRequest): Observable<SubmitWordResponse> {
    return this.http.post<SubmitWordResponse>(`${this.baseUrl}/submit`, request);
  }

  // --- Game Logic for Logged-in User ---
  async loadTodaySession(): Promise<void> {
    this._loadStatus.set('loading');
    try {
      const [sessionData, todayBoard] = await Promise.all([
        firstValueFrom(this.getTodaySessionApi()),
        firstValueFrom(this.puzzleService.getTodayPuzzle())
      ]);

      this._session.set(sessionData);
      this._board.set(todayBoard);
      this.clearInput();
      this._loadStatus.set('ready');
    } catch (err) {
      this._loadStatus.set('error');
    }
  }

  async submitWord(): Promise<ValidationResult> {
    const session = this._session();
    const word = this._currentInput().trim().toUpperCase();

    if (!session || this._loadStatus() !== 'ready' || !word) {
      return { isValid: false, pointsAwarded: 0, isMielegramma: false };
    }

    try {
      const response = await firstValueFrom(this.submitWordApi({ sessionId: session.id, word }));
      this.clearInput();

      if (response.success) {
        // Update state for a valid word
        this._session.update((curr) => {
          if (!curr) return null;
          const foundSet = new Set(curr.foundWords);
          foundSet.add(response.word);

          return {
            ...curr,
            currentScore: response.currentScore,
            currentRankLabel: response.currentRankLabel,
            foundWords: Array.from(foundSet)
          };
        });

        return { isValid: true, pointsAwarded: response.pointsEarned, isMielegramma: response.isMielegramma };
      } else {
        // The backend saved the invalid attempt. Immediately update local state for the invalid words list.
        this._session.update((curr) => {
          if (!curr) return null;
          const invalidSet = new Set(curr.invalidWords);
          invalidSet.add(word);

          return {
            ...curr,
            invalidWords: Array.from(invalidSet)
          };
        });

        return {
          isValid: false,
          pointsAwarded: 0,
          isMielegramma: false,
          errorType: (response.errorCode as ValidationErrorType) ?? undefined,
          message: response.errorMessage ?? undefined
        };
      }
    } catch (err) {
      this.clearInput();
      return {
        isValid: false,
        pointsAwarded: 0,
        isMielegramma: false,
        message: 'Server connection error'
      };
    }
  }

  // Centralized Input & Shuffle Management
  handleInput(rawChar: string) {
    return processInput({
      rawChar,
      isReady: this._loadStatus() === 'ready',
      currentInputSignal: this._currentInput,
      submitFn: () => this.submitWord(),
    });
  }

  deleteLastChar(): void {
    deleteLastChar(this._currentInput);
  }

  clearInput(): void {
    clearInput(this._currentInput);
  }

  shuffle(): void {
    shuffleOrder(this._shuffleOrder);
  }

  readonly displayCells = computed<Cell[]>(() => {
    const currentBoard = this._board();
    if (!currentBoard || !currentBoard.cells) return [];

    const centerCell = currentBoard.cells.find((c) => c.isCenter)!;
    const outerCells = currentBoard.cells.filter((c) => !c.isCenter);
    const order = this._shuffleOrder();

    const reorderedOuterCells = outerCells.map((cell, index) => {
      const newPositionIndex = order[index];
      return {
        ...cell,
        position: newPositionIndex as Cell['position'],
      };
    });

    return [centerCell, ...reorderedOuterCells];
  });
}