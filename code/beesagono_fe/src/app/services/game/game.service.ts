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
import { ScoreService } from '../score/score.service';
import { StatsService } from '../stats/stats.service';
import { RANK_TIERS } from '../../config/rank-tiers.config';
import { WordMapItem } from '../../models/word-map-item.model';

export function getTodayIsoString(date = new Date()): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

@Injectable({
    providedIn: 'root',
})
export class GameService {
    private storageService = inject(StorageService);
    private dictionaryService = inject(DictionaryService);
    private puzzleGeneratorService = inject(PuzzleGeneratorService);
    private scoreService = inject(ScoreService);
    private statsService = inject(StatsService);

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
        // Find the highest unlocked rank for this percentage
        const currentRank = this.reversedRankTiers.find(
            (tier) => percentage >= tier.threshold
        );
        return currentRank ?? RANK_TIERS[0];
    }

    readonly rank = computed<RankTier>(() => {
        const currentBoard = this._board();
        const currentScore = this.score();
        const maxScore = currentBoard ? currentBoard.maxScore : 0;

        return this.scoreService.getCurrentRank(currentScore, maxScore);
    });

    private readonly _board = signal<GameBoard | null>(null);
    private readonly _loadStatus = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');
    private readonly _loadError = signal<string | null>(null);
    private readonly _currentInput = signal<string>('');
    private readonly _foundWords = signal<string[]>([]);
    private readonly _shuffleOrder = signal<number[]>([1, 2, 3, 4, 5, 6]);
    private readonly _startTime = signal<number>(Date.now());
    private readonly _isStorageAvailable = signal<boolean>(true);
    private readonly _invalidWords = signal<string[]>([]);

    readonly board: Signal<GameBoard | null> = this._board.asReadonly();
    readonly loadStatus: Signal<'idle' | 'loading' | 'ready' | 'error'> = this._loadStatus.asReadonly();
    readonly loadError: Signal<string | null> = this._loadError.asReadonly();
    readonly currentInput: Signal<string> = this._currentInput.asReadonly();
    readonly foundWords: Signal<string[]> = this._foundWords.asReadonly();
    readonly isStorageAvailable: Signal<boolean> = this._isStorageAvailable.asReadonly();
    readonly invalidWords: Signal<string[]> = this._invalidWords.asReadonly();

    // Pre-calculated Set of possible words for O(1) lookups during validation.
    private readonly possibleWordsSet = computed<Set<string>>(() => {
        const currentBoard = this._board();
        return new Set(currentBoard?.possibleWords ?? []);
    });

    // Pre-calculated Set of mielegrammi for O(1) lookups.
    private readonly mielegrammiSet = computed<Set<string>>(() => {
        const currentBoard = this._board();
        return new Set(currentBoard?.mielegrammi ?? []);
    });

    readonly foundMielegrammi = computed(() => {
        const mSet = this.mielegrammiSet();
        return this._foundWords().filter((word) => mSet.has(word));
    });

    // Score calculation delegated to ScoreService.
    readonly score = computed(() => {
        const currentBoard = this._board();
        if (!currentBoard) return 0;
        return this.scoreService.calculateTotalScore(this._foundWords(), this.mielegrammiSet());
    });

    // Maximum achievable score for the active game session
    readonly maxScore = computed<number>(() => {
        const currentBoard = this._board();
        return currentBoard ? currentBoard.maxScore : 0;
    });

    // Total possible count of valid words in the active puzzle
    readonly totalPossibleWords = computed<number>(() => {
        const currentBoard = this._board();
        return currentBoard ? currentBoard.possibleWords.length : 0;
    });

    // Total possible count of mielegrammi in the active puzzle
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

    // Minimum points required to reach the current rank
    readonly currentRankScore = computed<number>(() => {
        const maxScore = this.maxScore();
        const currentRank = this.rank();
        return Math.ceil((maxScore * currentRank.threshold) / 100);
    });

    // Minimum points required to unlock the next rank
    readonly nextRankScore = computed<number>(() => {
        const maxScore = this.maxScore();
        const currentScore = this.score();

        const nextTier = RANK_TIERS.find((tier) => {
            const requiredPoints = Math.ceil((maxScore * tier.threshold) / 100);
            return requiredPoints > currentScore;
        });

        if (!nextTier) return maxScore;

        return Math.ceil((maxScore * nextTier.threshold) / 100);
    });

    readonly isQueenRank = computed<boolean>(() => {
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

    // Call this method when starting the game to initialize stats for the day
    initGame(): void {
        const todayIso = getTodayIsoString(new Date());
        this.statsService.recordGameStarted(todayIso);
    }

    constructor() {
        effect(() => {
            const currentBoard = this._board();
            const words = this._foundWords();
            const invalidWords = this._invalidWords();

            if (currentBoard && this._loadStatus() === 'ready') {
                const stateToSave: GameState & { rankLabel?: string } = {
                    version: 1,
                    date: currentBoard.date,
                    score: this.score(),
                    foundWords: words,
                    invalidWords: invalidWords,
                    foundMielegrammi: this.foundMielegrammi(),
                    isCompleted: this.isCompleted(),
                    startTime: this._startTime(),
                    lastUpdated: Date.now(),
                    rankLabel: this.rank().label,
                };

                this.storageService.save(`game:${currentBoard.date}`, stateToSave);
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

            const savedState = this.storageService.load<GameState>(`game:${todayIsoDate}`);

            if (savedState && savedState.version === 1) {
                const validWordsSet = new Set(generatedBoard.possibleWords);
                const sanitizedWords = Array.from(
                    new Set(savedState.foundWords.filter((w) => validWordsSet.has(w)))
                );
                this._foundWords.set(sanitizedWords);

                this._invalidWords.set(savedState.invalidWords ?? []);
                this._startTime.set(savedState.startTime ?? Date.now());
            } else {
                this._foundWords.set([]);
                this._invalidWords.set([]);
                this._startTime.set(Date.now());
                // Notify StatsService that a new game has started
                this.statsService.recordGameStarted(todayIsoDate);
            }

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

    handleInput(rawChar: string): ValidationResult | void {
        if (this._loadStatus() !== 'ready') return;

        const normalized = rawChar.toUpperCase();
        if (normalized === 'BACKSPACE') {
            this.deleteLastChar();
        } else if (normalized === 'ENTER') {
            return this.submitWord();
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

        const trackInvalid = (word: string) => {
            if (!this._invalidWords().includes(word)) {
                this._invalidWords.update((list) => [...list, word]);
            }
            this.clearInput();
        };

        if (inputWord.length < GAME_RULES.MIN_WORD_LENGTH) {
            trackInvalid(inputWord);
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
            trackInvalid(inputWord);
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
            trackInvalid(inputWord);
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'INVALID_LETTERS',
                message: 'Lettere non valide',
            };
        }

        if (this._foundWords().includes(inputWord)) {
            this.clearInput();
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'ALREADY_FOUND',
                message: 'Già trovata',
            };
        }

        if (!this.possibleWordsSet().has(inputWord)) {
            trackInvalid(inputWord);
            return {
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'NOT_IN_DICTIONARY',
                message: 'Non è nella lista delle parole',
            };
        }

        const isMielegramma = this.mielegrammiSet().has(inputWord);
        const points = this.scoreService.calculateWordPoints(inputWord, isMielegramma);

        // Calculate NEW list and NEW total score before updating
        const updatedWords = [...this._foundWords(), inputWord];

        // Update found words Signal
        this._foundWords.set(updatedWords);
        this.clearInput();

        // Calculate new total score atomically and safely
        const newTotalScore = this.scoreService.calculateTotalScore(
            updatedWords,
            this.mielegrammiSet()
        );

        // Notify StatsService with updated score
        this.statsService.recordProgress(
            currentBoard.date,
            newTotalScore,
            this.isCompleted(),
            this.rank().label
        );

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
        return getTodayIsoString();
    }

    readonly wordMap = computed(() => {
        const currentBoard = this._board();
        if (!currentBoard) return [];

        const validWords = currentBoard.possibleWords;
        const mSet = this.mielegrammiSet();
        const found = this._foundWords();

        return validWords.map((word: string) => ({
            word: word,
            length: word.length,
            initial: word.toUpperCase()[0],
            isFound: found.includes(word),
            isPangram: mSet.has(word)
        })).sort((a, b) => {
            if (a.length !== b.length) {
                return a.length - b.length;
            }
            return a.word.localeCompare(b.word);
        });
    });

    private readonly letterPalette = [
        '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
        '#FFEEAD', '#D4A5A5', '#9B59B6'
    ];

    readonly letterColors = computed(() => {
        const board = this.board();
        if (!board) return new Map<string, string>();

        const map = new Map<string, string>();
        board.cells.forEach((cell, index) => {
            map.set(cell.letter, this.letterPalette[index % this.letterPalette.length]);
        });
        return map;
    });
}