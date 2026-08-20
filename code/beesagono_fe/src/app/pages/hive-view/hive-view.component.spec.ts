import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HiveViewComponent } from './hive-view.component';
import { GameService } from '../../services/game/game.service';
import { WelcomeNoticeService } from '../../services/welcome-notice/welcome-notice.service';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { signal, NO_ERRORS_SCHEMA } from '@angular/core';
import { GameBoard } from '../../models/game-board.model';
import { Cell } from '../../models/cell.model';
import { ShareScorePayload } from '../../models/share-score.model';
import { RankTier } from '../../models/rank.model';
import { ValidationResult } from '../../models/validation.model';

describe('HiveViewComponent', () => {
    let component: HiveViewComponent;
    let fixture: ComponentFixture<HiveViewComponent>;

    const loadStatusSignal = signal<'idle' | 'loading' | 'ready' | 'error'>('ready');
    const loadErrorSignal = signal<string | null>(null);
    const boardSignal = signal<GameBoard | null>({
        date: '2026-08-12',
        seed: 'test-seed',
        cells: [
            { id: '0', letter: 'A', isCenter: true, position: 0 },
            { id: '1', letter: 'P', isCenter: false, position: 1 },
            { id: '2', letter: 'E', isCenter: false, position: 2 }
        ],
        possibleWords: ['APE'],
        mielegrammi: [],
        maxScore: 100
    });
    const isCompletedSignal = signal<boolean>(false);
    const currentInputSignal = signal<string>('');
    const displayCellsSignal = signal<Cell[]>([
        { id: '0', letter: 'A', isCenter: true, position: 0 },
        { id: '1', letter: 'P', isCenter: false, position: 1 },
        { id: '2', letter: 'E', isCenter: false, position: 2 }
    ]);
    const foundWordsSignal = signal<string[]>([]);
    const foundMielegrammiSignal = signal<string[]>([]);
    const invalidWordsSignal = signal<string[]>([]);
    const totalPossibleWordsSignal = signal<number>(10);
    const totalMielegrammiSignal = signal<number>(1);
    const scoreSignal = signal<number>(50);
    const maxScoreSignal = signal<number>(100);
    const rankSignal = signal<RankTier>({
        threshold: 0,
        label: '🌱 Iniziato'
    });

    const isNoticeOpenSignal = signal<boolean>(false);

    const mockPayload: ShareScorePayload = {
        date: '12/08/2026',
        score: 50,
        maxScore: 100,
        wordsFound: 5,
        totalWords: 10,
        mielegrammiFound: 1,
        totalMielegrammi: 1
    };

    let mockGameService: Partial<GameService>;
    let mockWelcomeNoticeService: Partial<WelcomeNoticeService>;

    beforeEach(async () => {
        vi.useFakeTimers();

        loadStatusSignal.set('ready');
        loadErrorSignal.set(null);
        currentInputSignal.set('');
        displayCellsSignal.set([
            { id: '0', letter: 'A', isCenter: true, position: 0 },
            { id: '1', letter: 'P', isCenter: false, position: 1 },
            { id: '2', letter: 'E', isCenter: false, position: 2 }
        ]);
        boardSignal.set({
            date: '2026-08-12',
            seed: 'test-seed',
            cells: [
                { id: '0', letter: 'A', isCenter: true, position: 0 },
                { id: '1', letter: 'P', isCenter: false, position: 1 },
                { id: '2', letter: 'E', isCenter: false, position: 2 }
            ],
            possibleWords: ['APE'],
            mielegrammi: [],
            maxScore: 100
        });
        isCompletedSignal.set(false);
        foundWordsSignal.set([]);
        foundMielegrammiSignal.set([]);
        invalidWordsSignal.set([]);
        totalPossibleWordsSignal.set(10);
        totalMielegrammiSignal.set(1);
        scoreSignal.set(50);
        maxScoreSignal.set(100);
        rankSignal.set({
            threshold: 0,
            label: '🌱 Iniziato'
        });

        isNoticeOpenSignal.set(false);

        mockGameService = {
            loadStatus: loadStatusSignal as any,
            loadError: loadErrorSignal as any,
            board: boardSignal as any,
            isCompleted: isCompletedSignal as any,
            currentInput: currentInputSignal as any,
            displayCells: displayCellsSignal as any,
            foundWords: foundWordsSignal as any,
            foundMielegrammi: foundMielegrammiSignal as any,
            invalidWords: invalidWordsSignal as any,
            totalPossibleWords: totalPossibleWordsSignal as any,
            totalMielegrammi: totalMielegrammiSignal as any,
            score: scoreSignal as any,
            maxScore: maxScoreSignal as any,
            rank: rankSignal as any,
            loadDailyGame: vi.fn(),
            retryLoadDailyGame: vi.fn(),
            checkDateRollover: vi.fn(),
            submitWord: vi.fn().mockReturnValue({
                isValid: true,
                pointsAwarded: 5,
                isMielegramma: false
            } as ValidationResult),
            deleteLastChar: vi.fn(),
            handleInput: vi.fn(),
            getShareScorePayload: vi.fn().mockReturnValue(mockPayload),
            shuffle: vi.fn()
        };

        mockWelcomeNoticeService = {
            isNoticeOpen: isNoticeOpenSignal as any,
            checkAndShowNotice: vi.fn(),
            dismissNotice: vi.fn()
        };

        await TestBed.configureTestingModule({
            imports: [HiveViewComponent],
            providers: [
                { provide: GameService, useValue: mockGameService },
                { provide: WelcomeNoticeService, useValue: mockWelcomeNoticeService }
            ],
            schemas: [NO_ERRORS_SCHEMA]
        }).compileComponents();

        fixture = TestBed.createComponent(HiveViewComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        await fixture.whenStable();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should create the component instance', () => {
        expect(component).toBeTruthy();
    });

    describe('Lifecycle & Focus Rollover', () => {
        it('should check welcome notice and call loadDailyGame on ngOnInit if loadStatus is idle', () => {
            loadStatusSignal.set('idle');
            component.ngOnInit();

            expect(mockWelcomeNoticeService.checkAndShowNotice).toHaveBeenCalled();
            expect(mockGameService.loadDailyGame).toHaveBeenCalled();
        });

        it('should check date rollover on ngOnInit if loadStatus is NOT idle', () => {
            loadStatusSignal.set('ready');
            component.ngOnInit();

            expect(mockWelcomeNoticeService.checkAndShowNotice).toHaveBeenCalled();
            expect(mockGameService.checkDateRollover).toHaveBeenCalled();
        });

        it('should check date rollover on window focus / document visibility change', () => {
            vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible');

            component.onWindowFocusOrVisibilityChange();

            expect(mockGameService.checkDateRollover).toHaveBeenCalled();
        });
    });

    describe('Computed Properties', () => {
        it('should compute the correct center letter from board', () => {
            // @ts-expect-error Accessing protected property for unit testing
            expect(component.centerLetter()).toBe('A');
        });

        it('should return empty string for centerLetter if board is null', () => {
            boardSignal.set(null);
            fixture.detectChanges();

            // @ts-expect-error Accessing protected property for unit testing
            expect(component.centerLetter()).toBe('');
        });

        it('should format date correctly in Italian format', () => {
            expect(component.formattedDate()).toContain('12');
            expect(component.formattedDate()).toContain('2026');
        });

        it('should return empty string for formattedDate if board is null', () => {
            boardSignal.set(null);
            fixture.detectChanges();

            expect(component.formattedDate()).toBe('');
        });

        it('should obtain endGamePayload from gameService', () => {
            // @ts-expect-error Accessing protected property for unit testing
            expect(component.endGamePayload()).toEqual(mockPayload);
        });
    });

    describe('Modal State Handlers', () => {
        it('should toggle help, stats, and end game modals', () => {
            component.openHelpModal();
            // @ts-expect-error Accessing protected property
            expect(component.isHelpModalOpen()).toBe(true);

            component.openStatsModal();
            // @ts-expect-error Accessing protected property
            expect(component.isStatsModalOpen()).toBe(true);

            component.openEndGame();
            // @ts-expect-error Accessing protected property
            expect(component.isEndGameModalOpen()).toBe(true);

            component.closeEndGame();
            // @ts-expect-error Accessing protected property
            expect(component.isEndGameModalOpen()).toBe(false);
        });
    });

    describe('Keyboard Listener Behavior', () => {
        it('should call submit on Enter key', () => {
            const submitSpy = vi.spyOn(component, 'submit');
            const event = new KeyboardEvent('keydown', { key: 'Enter' });

            component.handleKeyboardEvent(event);

            expect(submitSpy).toHaveBeenCalled();
        });

        it('should call deleteLastChar on Backspace key', () => {
            const event = new KeyboardEvent('keydown', { key: 'Backspace' });

            component.handleKeyboardEvent(event);

            expect(mockGameService.deleteLastChar).toHaveBeenCalled();
        });

        it('should pass single alphabetic key to handleInput', () => {
            const event = new KeyboardEvent('keydown', { key: 'a' });

            component.handleKeyboardEvent(event);

            expect(mockGameService.handleInput).toHaveBeenCalledWith('a');
        });

        it('should ignore keyboard input if a modal or notice is open', () => {
            isNoticeOpenSignal.set(true);
            fixture.detectChanges();

            const event = new KeyboardEvent('keydown', { key: 'Enter' });
            const submitSpy = vi.spyOn(component, 'submit');

            component.handleKeyboardEvent(event);

            expect(submitSpy).not.toHaveBeenCalled();
        });

        it('should ignore non-alphabetic keys or modifier shortcuts', () => {
            const event = new KeyboardEvent('keydown', { key: 'Shift' });

            component.handleKeyboardEvent(event);

            expect(mockGameService.handleInput).not.toHaveBeenCalled();
            expect(mockGameService.deleteLastChar).not.toHaveBeenCalled();
        });

        it('should ignore keyboard events if target is an input field', () => {
            const inputEl = document.createElement('input');
            const event = new KeyboardEvent('keydown', { key: 'Enter' });
            Object.defineProperty(event, 'target', { value: inputEl, enumerable: true });

            const submitSpy = vi.spyOn(component, 'submit');
            component.handleKeyboardEvent(event);

            expect(submitSpy).not.toHaveBeenCalled();
        });
    });

    describe('Submit Logic & Feedback', () => {
        it('should handle successful submit and display success message', () => {
            mockGameService.submitWord = vi.fn().mockReturnValue({
                isValid: true,
                pointsAwarded: 3,
                isMielegramma: false
            } as ValidationResult);

            component.submit();

            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackType()).toBe('success');
            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackMessage()).toBe('Ottimo!');

            vi.advanceTimersByTime(1500);

            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackMessage()).toBe('');
            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackType()).toBeNull();
        });

        it('should handle Mielegramma submit with special feedback message', () => {
            mockGameService.submitWord = vi.fn().mockReturnValue({
                isValid: true,
                pointsAwarded: 15,
                isMielegramma: true
            } as ValidationResult);

            component.submit();

            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackType()).toBe('success');
            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackMessage()).toBe('🎉 MIELEGRAMMA!');
        });

        it('should handle invalid submit and show error message from validation result', () => {
            mockGameService.submitWord = vi.fn().mockReturnValue({
                isValid: false,
                pointsAwarded: 0,
                isMielegramma: false,
                errorType: 'TOO_SHORT',
                message: 'Parola troppo corta'
            } as ValidationResult);

            component.submit();

            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackType()).toBe('error');
            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackMessage()).toBe('Parola troppo corta');

            vi.advanceTimersByTime(2000);

            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackMessage()).toBe('');
        });

        it('should auto-open end game modal when puzzle is completed on submit', () => {
            mockGameService.submitWord = vi.fn().mockReturnValue({
                isValid: true,
                pointsAwarded: 5,
                isMielegramma: false
            } as ValidationResult);
            isCompletedSignal.set(true);

            const openEndGameSpy = vi.spyOn(component, 'openEndGame');

            component.submit();

            vi.advanceTimersByTime(600);

            expect(openEndGameSpy).toHaveBeenCalled();
        });
    });

    describe('Share Results', () => {
        it('should copy formatted score text to clipboard and display feedback', async () => {
            const writeTextSpy = vi.fn().mockResolvedValue(undefined);
            Object.defineProperty(navigator, 'clipboard', {
                value: { writeText: writeTextSpy },
                writable: true,
                configurable: true
            });

            await component.shareResults();

            expect(writeTextSpy).toHaveBeenCalledWith(expect.stringContaining('🐝 Beesagono'));

            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackType()).toBe('success');

            // @ts-expect-error Accessing protected property for testing
            expect(component.feedbackMessage()).toBe('Risultati copiati negli appunti!');
        });
    });
});