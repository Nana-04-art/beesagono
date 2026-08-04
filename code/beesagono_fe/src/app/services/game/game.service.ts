import { Injectable, Signal, computed, effect, inject, signal } from '@angular/core';
import { GameBoard } from '../../models/game-board.model';
import { GameState } from '../../models/game-state.model';
import { RankTier } from '../../models/rank.model';
import { ValidationResult } from '../../models/validation.model';
import { ShareScorePayload } from '../../models/share-score.model';
import { Cell } from '../../models/cell.model';
import { GAME_RULES } from '../../config/game-rules.config';
import { getCenterLetter, getAvailableLetters } from '../../models/game-board.selectors';
import { DictionaryService } from '../dictionary/dictionary.service';
import { PuzzleGeneratorService } from '../puzzle-generator/puzzle-generator.service';
import { StorageService } from '../storage/storage.service';
import { RANK_TIERS } from '../../config/rank-tiers.config';

@Injectable({
    providedIn: 'root',
})
export class GameService {
    private storageService = inject(StorageService);
    private dictionaryService = inject(DictionaryService);
    private puzzleGeneratorService = inject(PuzzleGeneratorService);

    /**
     * Reference to the central rank tiers configuration.
     * Cached in descending order for efficient rank resolution.
     */
    private readonly reversedRankTiers: readonly RankTier[] = [...RANK_TIERS].reverse();

    /**
     * Returns the current rank based on percentage of max score achieved.
     * @param percentage - Score achieved relative to max possible score (0-100)
     */
    getRankForPercentage(percentage: number): RankTier {
        const currentRank = this.reversedRankTiers.find(
            (tier) => percentage >= tier.threshold
        );
        return currentRank ?? RANK_TIERS[0];
    }

    private readonly _board = signal<GameBoard | null>(null);
    private readonly _loadStatus = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');
    private readonly _loadError = signal<string | null>(null);
    private readonly _currentInput = signal<string>('');
    private readonly _foundWords = signal<string[]>([]);
    private readonly _shuffleOrder = signal<number[]>([1, 2, 3, 4, 5, 6]);
    private readonly _startTime = signal<number>(Date.now());
    private readonly _isStorageAvailable = signal<boolean>(true);

    readonly board: Signal<GameBoard | null> = this._board.asReadonly();
    readonly loadStatus: Signal<'idle' | 'loading' | 'ready' | 'error'> = this._loadStatus.asReadonly();
    readonly loadError: Signal<string | null> = this._loadError.asReadonly();
    readonly currentInput: Signal<string> = this._currentInput.asReadonly();
    readonly foundWords: Signal<string[]> = this._foundWords.asReadonly();
    readonly isStorageAvailable: Signal<boolean> = this._isStorageAvailable.asReadonly();

    /**
     * Pre-calculated Set of possible words for O(1) lookups during validation.
     */
    private readonly possibleWordsSet = computed<Set<string>>(() => {
        const currentBoard = this._board();
        return new Set(currentBoard?.possibleWords ?? []);
    });

    /**
     * Pre-calculated Set of mielegrammi for O(1) lookups.
     */
    private readonly mielegrammiSet = computed<Set<string>>(() => {
        const currentBoard = this._board();
        return new Set(currentBoard?.mielegrammi ?? []);
    });

    readonly foundMielegrammi = computed(() => {
        const mSet = this.mielegrammiSet();
        return this._foundWords().filter((word) => mSet.has(word));
    });

    readonly score = computed(() => {
        const currentBoard = this._board();
        if (!currentBoard) return 0;

        const mSet = this.mielegrammiSet();
        return this._foundWords().reduce((totalScore, word) => {
            let wordScore = word.length === 4 ? 1 : word.length;
            if (mSet.has(word)) {
                wordScore += GAME_RULES.MIELEGRAMMA_BONUS;
            }
            return totalScore + wordScore;
        }, 0);
    });

    /** Maximum achievable score for the active game session */
  readonly maxScore = computed<number>(() => {
    const currentBoard = this._board();
    return currentBoard ? currentBoard.maxScore : 0;
  });

  /** Total possible count of valid words in the active puzzle */
  readonly totalPossibleWords = computed<number>(() => {
    const currentBoard = this._board();
    return currentBoard ? currentBoard.possibleWords.length : 0;
  });

  /** Total possible count of mielegrammi in the active puzzle */
  readonly totalMielegrammi = computed<number>(() => {
    const currentBoard = this._board();
    return currentBoard ? currentBoard.mielegrammi.length : 0;
  });

    readonly isCompleted = computed(() => {
        const currentBoard = this._board();
        if (!currentBoard) return false;
        return (
            currentBoard.possibleWords.length > 0 &&
            this._foundWords().length === currentBoard.possibleWords.length
        );
    });

    readonly rank = computed<RankTier>(() => {
        const currentBoard = this._board();
        const currentScore = this.score();

        if (!currentBoard || currentBoard.maxScore === 0) {
            return RANK_TIERS[0];
        }

        const percentage = Math.floor((currentScore / currentBoard.maxScore) * 100);
        return this.getRankForPercentage(percentage);
    });

    /** Check if the player has achieved the highest possible rank */
  readonly isGeniusRank = computed<boolean>(() => {
    const currentBoard = this._board();
    if (!currentBoard || currentBoard.maxScore === 0) return false;
    return this.score() >= currentBoard.maxScore;
  });

    readonly displayCells = computed<Cell[]>(() => {
        const currentBoard = this._board();
        if (!currentBoard) return [];

        const centerCell = currentBoard.cells.find((c) => c.isCenter)!;
        const outerCells = currentBoard.cells.filter((c) => !c.isCenter);
        const order = this._shuffleOrder();

        const reorderedOuterCells = order.map((posIndex, arrayIdx) => {
            const originalCell = outerCells[posIndex - 1];
            return {
                ...originalCell,
                position: (arrayIdx + 1) as Cell['position'],
            };
        });

        return [centerCell, ...reorderedOuterCells];
    });

    constructor() {
        effect(() => {
            const currentBoard = this._board();
            const words = this._foundWords();

            if (currentBoard && this._loadStatus() === 'ready') {
                const stateToSave: GameState = {
                    version: 1,
                    date: currentBoard.date,
                    score: this.score(),
                    foundWords: words,
                    foundMielegrammi: this.foundMielegrammi(),
                    isCompleted: this.isCompleted(),
                    startTime: this._startTime(), // Preserves initial start time across saves
                    lastUpdated: Date.now(),
                };

                const saveSuccess = this.storageService.save(stateToSave);
                this._isStorageAvailable.set(this.storageService.isAvailable());
            }
        });
    }

    async loadDailyGame(): Promise<void> {
        if (this._loadStatus() === 'loading') return;

        this._loadStatus.set('loading');
        this._loadError.set(null);

        try {
            await this.dictionaryService.loadDictionary();
            const wordSet = this.dictionaryService.getWordSet();

            const todayIsoDate = this.getTodayIsoString();
            const generatedBoard = this.puzzleGeneratorService.generateDailyPuzzle(
                todayIsoDate,
                wordSet
            );

            this._board.set(generatedBoard);

            // Reconcile saved state or initialize new game session
            const savedState = this.storageService.load(todayIsoDate);
            this._isStorageAvailable.set(this.storageService.isAvailable());

            if (savedState && savedState.version === 1) {
                const validWordsSet = new Set(generatedBoard.possibleWords);
                const sanitizedWords = Array.from(
                    new Set(savedState.foundWords.filter((w) => validWordsSet.has(w)))
                );
                this._foundWords.set(sanitizedWords);
                this._startTime.set(savedState.startTime ?? Date.now());
            } else {
                this._foundWords.set([]);
                this._startTime.set(Date.now());
            }

            // Reset transient user state on fresh board load / midnight rollover
            this.clearInput();
            this._shuffleOrder.set([1, 2, 3, 4, 5, 6]);

            this._loadStatus.set('ready');
        } catch (err: any) {
            this._loadStatus.set('error');
            this._loadError.set(
                err?.message || 'Impossibile caricare i dati del gioco.'
            );
        }
    }

    async retryLoadDailyGame(): Promise<void> {
        if (this._loadStatus() === 'ready' || this._loadStatus() === 'loading') {
            return;
        }
        await this.loadDailyGame();
    }

    handleInput(rawChar: string): void {
        if (this._loadStatus() !== 'ready') return;

        const normalized = rawChar.toUpperCase();
        if (normalized === 'BACKSPACE') {
            this.deleteLastChar();
        } else if (normalized === 'ENTER') {
            this.submitWord();
        } else if (/^[A-Z]$/.test(normalized)) {
            this._currentInput.update((prev) => prev + normalized);
        }
    }

    deleteLastChar(): void {
        this._currentInput.update((prev) => prev.slice(0, -1));
    }

    clearInput(): void {
        this._currentInput.set('');
    }

    shuffle(): void {
        const currentOrder = [...this._shuffleOrder()];
        for (let i = currentOrder.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [currentOrder[i], currentOrder[j]] = [currentOrder[j], currentOrder[i]];
        }
        this._shuffleOrder.set(currentOrder);
    }

    submitWord(): ValidationResult {
        const inputWord = this._currentInput().trim().toUpperCase();
        const currentBoard = this._board();

        if (!currentBoard || this._loadStatus() !== 'ready') {
            return { isValid: false, pointsAwarded: 0, isMielegramma: false };
        }

        if (inputWord.length < GAME_RULES.MIN_WORD_LENGTH) {
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'TOO_SHORT',
                message: 'Parola troppo corta',
            };
        }

        const centerLetter = getCenterLetter(currentBoard);
        if (!inputWord.includes(centerLetter)) {
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'MISSING_CENTER',
                message: 'Manca la lettera centrale',
            };
        }

        const allowedLetters = getAvailableLetters(currentBoard);
        const hasOnlyAllowed = inputWord
            .split('')
            .every((char) => allowedLetters.includes(char));

        if (!hasOnlyAllowed) {
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'INVALID_LETTERS',
                message: 'Lettere non valide',
            };
        }

        if (this._foundWords().includes(inputWord)) {
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'ALREADY_FOUND',
                message: 'Già trovata',
            };
        }

        // O(1) set check instead of Array.includes scanning
        if (!this.possibleWordsSet().has(inputWord)) {
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'NOT_IN_DICTIONARY',
                message: 'Non è nella lista delle parole',
            };
        }

        const isMielegramma = this.mielegrammiSet().has(inputWord);
        let points = inputWord.length === 4 ? 1 : inputWord.length;
        if (isMielegramma) {
            points += GAME_RULES.MIELEGRAMMA_BONUS;
        }

        this._foundWords.update((words) => [...words, inputWord]);
        this.clearInput();

        return {
            isValid: true,
            pointsAwarded: points,
            isMielegramma,
        };
    }

    checkDateRollover(): void {
        const currentBoard = this._board();
        if (!currentBoard) return;

        const todayIsoDate = this.getTodayIsoString();
        if (currentBoard.date !== todayIsoDate) {
            this.loadDailyGame();
        }
    }

    getShareScorePayload(): ShareScorePayload {
        const currentBoard = this._board();
        const todayIsoDate = currentBoard ? currentBoard.date : this.getTodayIsoString();

        const [year, month, day] = todayIsoDate.split('-');
        const formattedDate = `${day}/${month}/${year}`;

        return {
            date: formattedDate,
            score: this.score(),
            maxScore: currentBoard ? currentBoard.maxScore : 0,
            wordsFound: this._foundWords().length,
            totalWords: currentBoard ? currentBoard.possibleWords.length : 0,
            mielegrammiFound: this.foundMielegrammi().length,
            totalMielegrammi: currentBoard ? currentBoard.mielegrammi.length : 0,
        };
    }

    private getTodayIsoString(): string {
        // Formats local calendar date to YYYY-MM-DD
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
}