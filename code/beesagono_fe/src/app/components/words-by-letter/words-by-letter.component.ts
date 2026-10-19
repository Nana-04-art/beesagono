import { Component, computed, effect, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { LetterGroup } from '../../models/game/letter-group.model';
import { WordMapComponent } from '../word-map/word-map.component';
import { WordMapItem } from '../../models/game/word-map-item.model';
import { GameFacadeService } from '../../services/game/game-facade.service';
import { buildWordMapForLetter } from '../../utils/word-map.utils';

@Component({
  selector: 'app-words-by-letter',
  standalone: true,
  imports: [NgClass, WordMapComponent],
  templateUrl: './words-by-letter.component.html',
  styleUrl: './words-by-letter.component.scss',
})
export class WordsByLetterComponent {
  readonly gameFacade = inject(GameFacadeService);

  private readonly _expandedLetters = signal<Set<string>>(new Set());
  readonly expandedLetters = this._expandedLetters.asReadonly();

  private readonly _showMapLetters = signal<Set<string>>(new Set());
  readonly showMapLetters = this._showMapLetters.asReadonly();

  // Track the signature of the last-loaded board to reset the accordion when the game changes
  private lastBoardKey = '';

  readonly totalFound = computed(() => this.gameFacade.foundWords().length);
  readonly totalPossible = computed(() => this.gameFacade.totalPossibleWords());
  readonly pangramsFound = computed(() => this.gameFacade.foundMielegrammi().length);
  readonly totalPangrams = computed(() => this.gameFacade.totalMielegrammi());

  readonly letterGroups = computed<LetterGroup[]>(() => {
    const board = this.gameFacade.board();
    if (!board) return [];

    const foundWords = this.gameFacade.foundWords();
    const foundMielegrammiSet = new Set(this.gameFacade.foundMielegrammi());

    const wordsMap = board.wordsCountByLetter || {};
    const mielegrammiMap = board.mielegrammiCountByLetter || {};

    return board.cells.map((cell) => {
      const letter = cell.letter.toUpperCase();
      const totalCount = wordsMap[letter] || 0;

      const foundWordsForLetter = foundWords.filter((w) => w.startsWith(letter));
      const foundPangramsForLetter = foundWordsForLetter.filter((w) => foundMielegrammiSet.has(w)).length;

      const wordItemsForLetter = buildWordMapForLetter(
        letter,
        totalCount,
        foundWordsForLetter
      );

      return {
        letter,
        isCenter: cell.isCenter,
        foundCount: foundWordsForLetter.length,
        totalCount: totalCount,
        foundPangrams: foundPangramsForLetter,
        totalPangrams: mielegrammiMap[letter] || 0,
        foundWords: foundWordsForLetter,
        wordItems: wordItemsForLetter
      };
    });
  });

  constructor() {
    effect(() => {
      const board = this.gameFacade.board();
      const groups = this.letterGroups();

      if (!board || groups.length === 0) return;

      const currentBoardKey = board.date ?? groups.map((g) => g.letter).join('');

      if (this.lastBoardKey !== currentBoardKey) {
        this.lastBoardKey = currentBoardKey;
        this._expandedLetters.set(new Set(groups.map((g) => g.letter)));
        this._showMapLetters.set(new Set());
      }
    });
  }

  toggleExpand(letter: string): void {
    this._expandedLetters.update((currentSet) => {
      const nextSet = new Set(currentSet);
      if (nextSet.has(letter)) {
        nextSet.delete(letter);
      } else {
        nextSet.add(letter);
      }
      return nextSet;
    });
  }

  toggleMapView(letter: string, event: Event): void {
    event.stopPropagation();
    if (!this._expandedLetters().has(letter)) {
      this._expandedLetters.update((currentSet) => new Set(currentSet).add(letter));
    }

    this._showMapLetters.update((currentSet) => {
      const nextSet = new Set(currentSet);
      if (nextSet.has(letter)) {
        nextSet.delete(letter);
      } else {
        nextSet.add(letter);
      }
      return nextSet;
    });
  }

  isMielegramma(word: string): boolean {
    const upperWord = word.toUpperCase();
    return upperWord.length >= 7 && new Set(upperWord).size === 7;
  }
}