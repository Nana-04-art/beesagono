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

@Injectable({
    providedIn: 'root',
})
export class GameService {
    private storageService = inject(StorageService);
    private dictionaryService = inject(DictionaryService);
    private puzzleGeneratorService = inject(PuzzleGeneratorService);

    private readonly RANK_TIERS: RankTier[] = [
        { threshold: 100, label: 'Ape Regina' },
        { threshold: 70, label: 'Maestro' },
        { threshold: 40, label: 'Genio' },
        { threshold: 25, label: 'Eccellente' },
        { threshold: 15, label: 'Esperto' },
        { threshold: 8, label: 'Avanzato' },
        { threshold: 5, label: 'Principiante' },
        { threshold: 2, label: 'Mente Fresca' },
        { threshold: 0, label: 'Iniziato' },
    ];

    private readonly _board = signal<GameBoard | null>(null);
    private readonly _loadStatus = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');
    private readonly _loadError = signal<string | null>(null);
    private readonly _currentInput = signal<string>('');
    private readonly _foundWords = signal<string[]>([]);
    private readonly _shuffleOrder = signal<number[]>([1, 2, 3, 4, 5, 6]);

    readonly board: Signal<GameBoard | null> = this._board.asReadonly();
    readonly loadStatus: Signal<'idle' | 'loading' | 'ready' | 'error'> = this._loadStatus.asReadonly();
    readonly loadError: Signal<string | null> = this._loadError.asReadonly();
    readonly currentInput: Signal<string> = this._currentInput.asReadonly();
    readonly foundWords: Signal<string[]> = this._foundWords.asReadonly();

    readonly isStorageAvailable = computed(() => this.storageService.isAvailable());

    readonly foundMielegrammi = computed(() => {
        const currentBoard = this._board();
        if (!currentBoard) return [];
        return this._foundWords().filter((word) => currentBoard.mielegrammi.includes(word));
    });

    readonly score = computed(() => {
        const currentBoard = this._board();
        if (!currentBoard) return 0;

        return this._foundWords().reduce((totalScore, word) => {
            // 4-letter words earn 1 point, longer words earn 1 point per letter
            let wordScore = word.length === 4 ? 1 : word.length;
            if (currentBoard.mielegrammi.includes(word)) {
                wordScore += GAME_RULES.MIELEGRAMMA_BONUS;
            }
            return totalScore + wordScore;
        }, 0);
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
            return this.RANK_TIERS[this.RANK_TIERS.length - 1];
        }

        const percentage = Math.floor((currentScore / currentBoard.maxScore) * 100);
        return (
            this.RANK_TIERS.find((tier) => percentage >= tier.threshold) ??
            this.RANK_TIERS[this.RANK_TIERS.length - 1]
        );
    });

    readonly displayCells = computed<Cell[]>(() => {
        const currentBoard = this._board();
        if (!currentBoard) return [];

        const centerCell = currentBoard.cells.find((c) => c.isCenter)!;
        const outerCells = currentBoard.cells.filter((c) => !c.isCenter);
        const order = this._shuffleOrder();

        // Map radial outer positions (1-6) using the active shuffle permutation
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
                    startTime: Date.now(),
                    lastUpdated: Date.now(),
                };
                this.storageService.save(stateToSave);
            }
        });
    }

    async loadDailyGame(): Promise<void> {
        if (this._loadStatus() === 'loading') return;

        this._loadStatus.set('loading');
        this._loadError.set(null);

        try {
            const wordList = await this.dictionaryService.loadDictionary();
            const wordSet = this.dictionaryService.getWordSet();

            const todayIsoDate = this.getTodayIsoString();
            const generatedBoard = this.puzzleGeneratorService.generateDailyPuzzle(
                todayIsoDate,
                wordSet
            );

            this._board.set(generatedBoard);

            // Reconcile and sanitize loaded words against current generated board
            const savedState = this.storageService.load(todayIsoDate);
            if (savedState && savedState.version === 1) {
                const sanitizedWords = Array.from(
                    new Set(
                        savedState.foundWords.filter((w) =>
                            generatedBoard.possibleWords.includes(w)
                        )
                    )
                );
                this._foundWords.set(sanitizedWords);
            } else {
                this._foundWords.set([]);
            }

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
        if (/^[A-Z]$/.test(normalized)) {
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
        // Fisher-Yates shuffle restricted to outer positions (1-6)
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

        if (!currentBoard.possibleWords.includes(inputWord)) {
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'NOT_IN_DICTIONARY',
                message: 'Non è nella lista delle parole',
            };
        }

        const isMielegramma = currentBoard.mielegrammi.includes(inputWord);
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