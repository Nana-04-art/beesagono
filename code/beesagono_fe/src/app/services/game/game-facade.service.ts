import { Injectable, Signal, computed, inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { GameService } from './game.service';
import { GameApiService } from './game-api.service';
import { ScoreService } from '../score/score.service';
import { GameBoard } from '../../models/game/game-board.model';
import { WordMapItem } from '../../models/game/word-map-item.model';
import { ValidationResult } from '../../models/game/validation.model';
import { ShareScorePayload } from '../../models/game/share-score.model';
import { RankTier } from '../../models/game/rank.model';

@Injectable({
  providedIn: 'root'
})
/**
 * Unified Facade for Game Operations.
 * Dispatches state and actions seamlessly between Guest Mode (GameService) 
 * and Logged-in User Mode (GameApiService).
 */
export class GameFacadeService {
  private readonly authService = inject(AuthService);
  private readonly guestGame = inject(GameService);
  private readonly apiGame = inject(GameApiService);
  private readonly scoreService = inject(ScoreService);

  readonly isLoggedIn = this.authService.isLoggedIn;

  // --- REACTIVE STATE SIGNALS ---
  readonly board: Signal<GameBoard | null> = computed(() =>
    this.isLoggedIn() ? this.apiGame.board() : this.guestGame.board()
  );

  readonly loadStatus = computed(() =>
    this.isLoggedIn() ? this.apiGame.loadStatus() : this.guestGame.loadStatus()
  );

  readonly loadError = computed(() => {
    if (this.isLoggedIn()) {
      return this.loadStatus() === 'error' ? 'Errore nel caricamento della sessione dal server.' : null;
    }
    return this.guestGame.loadError();
  });

  readonly currentInput = computed(() =>
    this.isLoggedIn() ? this.apiGame.currentInput() : this.guestGame.currentInput()
  );

  readonly foundWords = computed(() =>
    this.isLoggedIn() ? this.apiGame.foundWords() : this.guestGame.foundWords()
  );

  readonly invalidWords = computed(() =>
    this.isLoggedIn() ? this.apiGame.invalidWords() : this.guestGame.invalidWords()
  );

  readonly wordMap: Signal<WordMapItem[]> = computed(() =>
    this.isLoggedIn() ? this.apiGame.wordMap() : this.guestGame.wordMap()
  );

  readonly letterColors = computed(() =>
    this.isLoggedIn() ? this.apiGame.letterColors() : this.guestGame.letterColors()
  );

  readonly displayCells = computed(() =>
    this.isLoggedIn() ? this.apiGame.displayCells() : this.guestGame.displayCells()
  );

  // --- SCORE & RANK COMPUTATIONS ---
  readonly score = computed(() => {
    if (this.isLoggedIn()) {
      return this.apiGame.session()?.currentScore ?? 0;
    }
    return this.guestGame.score();
  });

  readonly maxScore = computed(() => this.board()?.maxScore ?? 0);

  readonly rank = computed<RankTier>(() => {
    const currentBoard = this.board();
    const currentScore = this.score();
    return this.scoreService.getCurrentRank(currentScore, currentBoard?.maxScore ?? 0);
  });

  // Utilizziamo i totali aggregati sicuri forniti dal GameBoard anziché possibleWords
  readonly totalPossibleWords = computed(() => this.board()?.totalWordsCount ?? 0);
  readonly totalMielegrammi = computed(() => this.board()?.totalMielegrammiCount ?? 0);

  readonly foundMielegrammi = computed(() => {
    return this.foundWords().filter((word) => new Set(word.toUpperCase()).size === 7);
  });

  readonly isCompleted = computed(() => {
    const currentBoard = this.board();
    if (!currentBoard || currentBoard.maxScore === 0) return false;
    return this.score() >= currentBoard.maxScore;
  });

  // --- ACTIONS & EVENTS ---
  async loadDailyGame(): Promise<void> {
    if (this.isLoggedIn()) {
      await this.apiGame.loadTodaySession();
    } else {
      await this.guestGame.loadDailyGame();
    }
  }

  checkDateRollover(): void {
    if (!this.isLoggedIn()) {
      this.guestGame.checkDateRollover();
    }
  }

  async submitWord(): Promise<ValidationResult> {
    if (this.isLoggedIn()) {
      return await this.apiGame.submitWord();
    }
    return await this.guestGame.submitWord();
  }

  async handleInput(rawChar: string): Promise<ValidationResult | void> {
    if (this.isLoggedIn()) {
      return await this.apiGame.handleInput(rawChar);
    }
    return this.guestGame.handleInput(rawChar);
  }

  deleteLastChar(): void {
    this.isLoggedIn() ? this.apiGame.deleteLastChar() : this.guestGame.deleteLastChar();
  }

  clearInput(): void {
    this.isLoggedIn() ? this.apiGame.clearInput() : this.guestGame.clearInput();
  }

  shuffle(): void {
    this.isLoggedIn() ? this.apiGame.shuffle() : this.guestGame.shuffle();
  }

  getShareScorePayload(): ShareScorePayload {
    const currentBoard = this.board();
    return {
      date: currentBoard?.date ?? '',
      score: this.score(),
      maxScore: this.maxScore(),
      wordsFound: this.foundWords().length,
      totalWords: this.totalPossibleWords(),
      mielegrammiFound: this.foundMielegrammi().length,
      totalMielegrammi: this.totalMielegrammi()
    };
  }
}