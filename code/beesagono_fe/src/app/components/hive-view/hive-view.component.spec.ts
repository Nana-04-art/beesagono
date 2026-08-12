import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HiveViewComponent } from './hive-view.component';
import { GameService } from '../../services/game/game.service';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { signal } from '@angular/core';
import { GameBoard } from '../../models/game-board.model';
import { Cell } from '../../models/cell.model';
import { ShareScorePayload } from '../../models/share-score.model';

describe('HiveViewComponent', () => {
  let component: HiveViewComponent;
  let fixture: ComponentFixture<HiveViewComponent>;

  // Signals simulati per GameService
  const loadStatusSignal = signal<'idle' | 'loading' | 'ready' | 'error'>('ready');
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

  beforeEach(async () => {
    vi.useFakeTimers();

    loadStatusSignal.set('ready');
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

    mockGameService = {
      loadStatus: loadStatusSignal,
      board: boardSignal,
      isCompleted: isCompletedSignal,
      currentInput: currentInputSignal,
      displayCells: displayCellsSignal,
      foundWords: foundWordsSignal,
      foundMielegrammi: foundMielegrammiSignal,
      invalidWords: invalidWordsSignal,
      totalPossibleWords: totalPossibleWordsSignal,
      totalMielegrammi: totalMielegrammiSignal,
      loadDailyGame: vi.fn(),
      checkDateRollover: vi.fn(),
      submitWord: vi.fn().mockReturnValue({ isValid: true, message: 'Ottimo!', isMielegramma: false }),
      deleteLastChar: vi.fn(),
      handleInput: vi.fn(),
      getShareScorePayload: vi.fn().mockReturnValue(mockPayload)
    };

    await TestBed.configureTestingModule({
      imports: [HiveViewComponent],
      providers: [
        { provide: GameService, useValue: mockGameService }
      ]
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
    it('should call loadDailyGame on ngOnInit if loadStatus is idle', () => {
      loadStatusSignal.set('idle');
      component.ngOnInit();
      expect(mockGameService.loadDailyGame).toHaveBeenCalled();
    });

    it('should call checkDateRollover on ngOnInit if loadStatus is NOT idle', () => {
      loadStatusSignal.set('ready');
      component.ngOnInit();
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
      // @ts-expect-error Accessing protected property for unit test
      expect(component.centerLetter()).toBe('A');
    });

    it('should return empty string for centerLetter if board is null', () => {
      boardSignal.set(null);
      fixture.detectChanges();
      // @ts-expect-error Accessing protected property for unit test
      expect(component.centerLetter()).toBe('');
    });

    it('should obtain endGamePayload from gameService', () => {
      // @ts-expect-error Accessing protected property for unit test
      expect(component.endGamePayload()).toEqual(mockPayload);
    });
  });

  describe('Modal State Handlers', () => {
    it('should toggle help, stats, and end game modals', () => {
      // Modale Help
      component.openHelpModal();
      // @ts-expect-error Accessing protected property for test
      expect(component.isHelpModalOpen()).toBe(true);

      // Modale Stats
      component.openStatsModal();
      // @ts-expect-error Accessing protected property for test
      expect(component.isStatsModalOpen()).toBe(true);

      // Modale End Game
      component.openEndGame();
      // @ts-expect-error Accessing protected property for test
      expect(component.isEndGameModalOpen()).toBe(true);

      component.closeEndGame();
      // @ts-expect-error Accessing protected property for test
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

    it('should ignore non-alphabetic keys or modifier shortcuts', () => {
      const event = new KeyboardEvent('keydown', { key: 'Shift' });

      component.handleKeyboardEvent(event);

      expect(mockGameService.handleInput).not.toHaveBeenCalled();
      expect(mockGameService.deleteLastChar).not.toHaveBeenCalled();
    });

    it('should ignore keyboard events if loadStatus is NOT ready', () => {
      loadStatusSignal.set('loading');
      fixture.detectChanges();

      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      const submitSpy = vi.spyOn(component, 'submit');

      component.handleKeyboardEvent(event);

      expect(submitSpy).not.toHaveBeenCalled();
    });
  });

  describe('Submit Logic & Feedback', () => {
    it('should handle successful submit and display success message', () => {
      mockGameService.submitWord = vi.fn().mockReturnValue({
        isValid: true,
        message: 'Ottimo!',
        isMielegramma: false
      });

      component.submit();

      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackType()).toBe('success');
      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackMessage()).toBe('Ottimo!');

      vi.advanceTimersByTime(1500);

      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackMessage()).toBe('');
      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackType()).toBeNull();
    });

    it('should handle Mielegramma submit with special feedback message', () => {
      mockGameService.submitWord = vi.fn().mockReturnValue({
        isValid: true,
        message: '🎉 MIELEGRAMMA!',
        isMielegramma: true
      });

      component.submit();

      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackType()).toBe('success');
      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackMessage()).toBe('🎉 MIELEGRAMMA!');
    });

    it('should handle invalid submit and show error message', () => {
      mockGameService.submitWord = vi.fn().mockReturnValue({
        isValid: false,
        message: 'Parola troppo corta',
        isMielegramma: false
      });

      component.submit();

      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackType()).toBe('error');
      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackMessage()).toBe('Parola troppo corta');

      vi.advanceTimersByTime(2000);

      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackMessage()).toBe('');
    });

    it('should auto-open end game modal when puzzle is completed on submit', () => {
      mockGameService.submitWord = vi.fn().mockReturnValue({
        isValid: true,
        message: 'Ottimo!',
        isMielegramma: false
      });
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
      Object.assign(navigator, {
        clipboard: {
          writeText: writeTextSpy
        }
      });

      component.shareResults();

      expect(writeTextSpy).toHaveBeenCalledWith(
        expect.stringContaining('🐝 Beesagono (12/08/2026)\nPunti: 50/100')
      );

      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackType()).toBe('success');
      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackMessage()).toBe('Risultati copiati negli appunti!');

      vi.advanceTimersByTime(2000);

      // @ts-expect-error Accessing protected property for test
      expect(component.feedbackMessage()).toBe('');
    });
  });
});