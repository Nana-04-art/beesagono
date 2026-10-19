import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { describe, beforeEach, it, expect } from 'vitest';

import { WordsByLetterComponent } from './words-by-letter.component';
import { GameFacadeService } from '../../services/game/game-facade.service';
import { GameBoard } from '../../models/game/game-board.model';
import { WordMapItem } from '../../models/game/word-map-item.model';
import { getTodayIsoString } from '../../utils/game-state.validator';

class MockGameFacadeService {
  readonly board: WritableSignal<GameBoard | null> = signal<GameBoard | null>(null);
  readonly foundWords: WritableSignal<string[]> = signal<string[]>([]);
  readonly foundMielegrammi: WritableSignal<string[]> = signal<string[]>([]);
  readonly totalPossibleWords: WritableSignal<number> = signal<number>(0);
  readonly totalMielegrammi: WritableSignal<number> = signal<number>(0);
  readonly wordMap: WritableSignal<WordMapItem[]> = signal<WordMapItem[]>([]);
}

describe('WordsByLetterComponent', () => {
  const todayStr = getTodayIsoString(new Date());

  let component: WordsByLetterComponent;
  let fixture: ComponentFixture<WordsByLetterComponent>;
  let mockGameFacade: MockGameFacadeService;

  const sampleBoard: GameBoard = {
    date: todayStr,
    seed: 'test-seed',
    cells: [
      { id: '0', letter: 'A', isCenter: true, position: 0 },
      { id: '1', letter: 'B', isCenter: false, position: 1 },
      { id: '2', letter: 'C', isCenter: false, position: 2 }
    ],
    maxScore: 100,
    totalWordsCount: 4,
    totalMielegrammiCount: 1,
    wordsCountByLetter: { A: 2, B: 1, C: 1 },
    mielegrammiCountByLetter: { A: 1, B: 0, C: 0 }
  };

  beforeEach(async () => {
    mockGameFacade = new MockGameFacadeService();

    await TestBed.configureTestingModule({
      imports: [WordsByLetterComponent],
      providers: [{ provide: GameFacadeService, useValue: mockGameFacade }]
    }).compileComponents();

    fixture = TestBed.createComponent(WordsByLetterComponent);
    component = fixture.componentInstance;
  });

  it('should create the component instance', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('Computed Counters', () => {
    it('should reflect total counters from GameFacadeService', () => {
      mockGameFacade.foundWords.set(['APE', 'BARCA']);
      mockGameFacade.totalPossibleWords.set(10);
      mockGameFacade.foundMielegrammi.set(['MIELEGRAMMA']);
      mockGameFacade.totalMielegrammi.set(2);

      fixture.detectChanges();

      expect(component.totalFound()).toBe(2);
      expect(component.totalPossible()).toBe(10);
      expect(component.pangramsFound()).toBe(1);
      expect(component.totalPangrams()).toBe(2);
    });
  });

  describe('Letter Groups Computation', () => {
    it('should return empty array when board is null', () => {
      mockGameFacade.board.set(null);
      fixture.detectChanges();

      expect(component.letterGroups()).toEqual([]);
    });

    it('should compute letter groups, filtering words and pangrams by starting letter', () => {
      mockGameFacade.board.set(sampleBoard);
      mockGameFacade.foundWords.set(['APE', 'AMORE']);
      mockGameFacade.foundMielegrammi.set(['AMORE']);

      fixture.detectChanges();

      const groups = component.letterGroups();
      expect(groups.length).toBe(3);

      const groupA = groups.find((g) => g.letter === 'A');
      expect(groupA).toBeDefined();
      expect(groupA?.isCenter).toBe(true);
      expect(groupA?.totalCount).toBe(2);
      expect(groupA?.foundCount).toBe(2);
      expect(groupA?.totalPangrams).toBe(1);
      expect(groupA?.foundPangrams).toBe(1);
      expect(groupA?.foundWords).toEqual(['APE', 'AMORE']);

      const groupB = groups.find((g) => g.letter === 'B');
      expect(groupB?.totalCount).toBe(1);
      expect(groupB?.foundCount).toBe(0);
      expect(groupB?.foundWords).toEqual([]);
    });
  });

  describe('Expansion Logic & Effect Initializer', () => {
    it('should expand all letters on initial board load', async () => {
      mockGameFacade.board.set(sampleBoard);

      fixture.detectChanges();
      await fixture.whenStable();

      const expanded = component.expandedLetters();
      expect(expanded.has('A')).toBe(true);
      expect(expanded.has('B')).toBe(true);
      expect(expanded.has('C')).toBe(true);
    });

    it('should toggle expanded state when toggleExpand is called', async () => {
      mockGameFacade.board.set(sampleBoard);
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
      mockGameFacade.board.set(sampleBoard);
      fixture.detectChanges();
      await fixture.whenStable();

      // User closes 'A'
      component.toggleExpand('A');
      expect(component.expandedLetters().has('A')).toBe(false);

      // We update the words found in the GameService (triggering the effect).
      mockGameFacade.foundWords.set(['APE']);
      fixture.detectChanges();
      await fixture.whenStable();

      // The user's choice ('A' closed) should not be overwritten
      expect(component.expandedLetters().has('A')).toBe(false);
    });
  });

  describe('isMielegramma Helper Method', () => {
    it('should return false for words with fewer than 7 unique letters', () => {
      expect(component.isMielegramma('AMORE')).toBe(false);
      expect(component.isMielegramma('APE')).toBe(false);
    });

    it('should return true for words with at least 7 unique letters', () => {
      expect(component.isMielegramma('mielegramma')).toBe(true);
      expect(component.isMielegramma('ABCDEFG')).toBe(true);
    });
  });
});