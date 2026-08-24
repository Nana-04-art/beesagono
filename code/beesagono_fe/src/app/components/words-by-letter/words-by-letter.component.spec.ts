import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { vi } from 'vitest';

import { WordsByLetterComponent } from './words-by-letter.component';
import { GameService } from '../../services/game/game.service';
import { GameBoard } from '../../models/game-board.model';

class MockGameService {
  readonly board: WritableSignal<GameBoard | null> = signal<GameBoard | null>(null);
  readonly foundWords: WritableSignal<string[]> = signal<string[]>([]);
  readonly foundMielegrammi: WritableSignal<string[]> = signal<string[]>([]);
  readonly totalPossibleWords: WritableSignal<number> = signal<number>(0);
  readonly totalMielegrammi: WritableSignal<number> = signal<number>(0);
}

describe('WordsByLetterComponent', () => {
  let component: WordsByLetterComponent;
  let fixture: ComponentFixture<WordsByLetterComponent>;
  let mockGameService: MockGameService;

  const sampleBoard: GameBoard = {
    date: '2026-08-07',
    seed: 'test-seed',
    cells: [
      { id: '0', letter: 'A', isCenter: true, position: 0 },
      { id: '1', letter: 'B', isCenter: false, position: 1 },
      { id: '2', letter: 'C', isCenter: false, position: 2 }
    ],
    possibleWords: ['APE', 'AMORE', 'BARCA', 'CASA'],
    mielegrammi: ['AMORE'],
    maxScore: 100
  };

  beforeEach(async () => {
    mockGameService = new MockGameService();

    await TestBed.configureTestingModule({
      imports: [WordsByLetterComponent],
      providers: [{ provide: GameService, useValue: mockGameService }]
    }).compileComponents();

    fixture = TestBed.createComponent(WordsByLetterComponent);
    component = fixture.componentInstance;
  });

  it('should create the component instance', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('Computed Counters', () => {
    it('should reflect total counters from GameService', () => {
      mockGameService.foundWords.set(['APE', 'BARCA']);
      mockGameService.totalPossibleWords.set(10);
      mockGameService.foundMielegrammi.set(['AMORE']);
      mockGameService.totalMielegrammi.set(2);

      fixture.detectChanges();

      expect(component.totalFound()).toBe(2);
      expect(component.totalPossible()).toBe(10);
      expect(component.pangramsFound()).toBe(1);
      expect(component.totalPangrams()).toBe(2);
    });
  });

  describe('Letter Groups Computation', () => {
    it('should return empty array when board is null', () => {
      mockGameService.board.set(null);
      fixture.detectChanges();

      expect(component.letterGroups()).toEqual([]);
    });

    it('should compute letter groups, filtering words and pangrams by starting letter', () => {
      mockGameService.board.set(sampleBoard);
      mockGameService.foundWords.set(['APE', 'AMORE']);
      mockGameService.foundMielegrammi.set(['AMORE']);

      fixture.detectChanges();

      const groups = component.letterGroups();
      expect(groups.length).toBe(3);

      const groupA = groups.find((g) => g.letter === 'A');
      expect(groupA).toBeDefined();
      expect(groupA?.isCenter).toBe(true);
      expect(groupA?.totalCount).toBe(2); // 'APE', 'AMORE'
      expect(groupA?.foundCount).toBe(2); // 'APE', 'AMORE'
      expect(groupA?.totalPangrams).toBe(1); // 'AMORE'
      expect(groupA?.foundPangrams).toBe(1); // 'AMORE'
      expect(groupA?.foundWords).toEqual(['APE', 'AMORE']);

      // Gruppo 'B'
      const groupB = groups.find((g) => g.letter === 'B');
      expect(groupB?.totalCount).toBe(1); // 'BARCA'
      expect(groupB?.foundCount).toBe(0);
      expect(groupB?.foundWords).toEqual([]);
    });
  });

  describe('Expansion Logic & Effect Initializer', () => {
    it('should expand all letters on initial board load', async () => {
      mockGameService.board.set(sampleBoard);

      fixture.detectChanges();
      await fixture.whenStable();

      const expanded = component.expandedLetters();
      expect(expanded.has('A')).toBe(true);
      expect(expanded.has('B')).toBe(true);
      expect(expanded.has('C')).toBe(true);
    });

    it('should toggle expanded state when toggleExpand is called', async () => {
      mockGameService.board.set(sampleBoard);
      fixture.detectChanges();
      await fixture.whenStable();

      // Closes 'A'
      component.toggleExpand('A');
      expect(component.expandedLetters().has('A')).toBe(false);

      // Reopens 'A'
      component.toggleExpand('A');
      expect(component.expandedLetters().has('A')).toBe(true);
    });

    it('should preserve custom user toggles when board updates after initialization', async () => {
      mockGameService.board.set(sampleBoard);
      fixture.detectChanges();
      await fixture.whenStable();

      // User closes 'A'
      component.toggleExpand('A');
      expect(component.expandedLetters().has('A')).toBe(false);

      // We update the words found in the GameService (triggering the effect).
      mockGameService.foundWords.set(['APE']);
      fixture.detectChanges();
      await fixture.whenStable();

      // The user's choice ('A' closed) should not be overwritten
      expect(component.expandedLetters().has('A')).toBe(false);
    });
  });

  describe('isMielegramma Helper Method', () => {
    it('should return false if board or board.mielegrammi is null', () => {
      mockGameService.board.set(null);
      fixture.detectChanges();

      expect(component.isMielegramma('AMORE')).toBe(false);
    });

    it('should return true for words present in board.mielegrammi (case-insensitive check)', () => {
      mockGameService.board.set(sampleBoard);
      fixture.detectChanges();

      expect(component.isMielegramma('amore')).toBe(true);
      expect(component.isMielegramma('APE')).toBe(false);
    });
  });
});