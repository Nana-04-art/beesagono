import { Component, computed, effect, inject, signal } from '@angular/core';
import { GameService } from '../../services/game/game.service';
import { LetterGroup } from '../../models/letter-group.model';
import { NgClass } from '@angular/common';
import { WordMapComponent } from '../word-map/word-map.component';
import { WordMapItem } from '../../models/word-map-item.model';

@Component({
  selector: 'app-words-by-letter',
  imports: [NgClass, WordMapComponent],
  templateUrl: './words-by-letter.component.html',
  styleUrl: './words-by-letter.component.scss',
})
export class WordsByLetterComponent {
  gameService = inject(GameService);

  private readonly _expandedLetters = signal<Set<string>>(new Set());
  readonly expandedLetters = this._expandedLetters.asReadonly();

  private readonly _showMapLetters = signal<Set<string>>(new Set());
  readonly showMapLetters = this._showMapLetters.asReadonly();

  // Flag to prevent overwriting user preferences after the first launch.
  private isInitialized = false;

  readonly totalFound = computed(() => this.gameService.foundWords().length);
  readonly totalPossible = computed(() => this.gameService.totalPossibleWords());
  readonly pangramsFound = computed(() => this.gameService.foundMielegrammi().length);
  readonly totalPangrams = computed(() => this.gameService.totalMielegrammi());

  readonly letterGroups = computed<LetterGroup[]>(() => {
    const board = this.gameService.board();
    if (!board) return [];

    const foundWords = this.gameService.foundWords();
    const foundMielegrammiSet = new Set(this.gameService.foundMielegrammi());
    const possibleWords = board.possibleWords ?? [];
    const mielegrammiSet = new Set(board.mielegrammi ?? []);

    // Retrieve the global item map from the service
    const globalWordMap = this.gameService.wordMap();

    return board.cells.map((cell) => {
      const letter = cell.letter;

      const totalWordsForLetter = possibleWords.filter((w) => w.startsWith(letter));
      const foundWordsForLetter = foundWords.filter((w) => w.startsWith(letter));

      const totalPangramsForLetter = totalWordsForLetter.filter((w) => mielegrammiSet.has(w)).length;
      const foundPangramsForLetter = foundWordsForLetter.filter((w) => foundMielegrammiSet.has(w)).length;

      // Filter the map items for this letter only
      const wordItemsForLetter = globalWordMap.filter((item: WordMapItem) => item.initial === letter);

      return {
        letter,
        isCenter: cell.isCenter,
        foundCount: foundWordsForLetter.length,
        totalCount: totalWordsForLetter.length,
        foundPangrams: foundPangramsForLetter,
        totalPangrams: totalPangramsForLetter,
        foundWords: foundWordsForLetter,
        wordItems: wordItemsForLetter
      };
    });
  });

  constructor() {
    effect(() => {
      const groups = this.letterGroups();
      if (!this.isInitialized && groups.length > 0) {
        this._expandedLetters.set(new Set(groups.map((g) => g.letter)));
        this.isInitialized = true;
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
    const board = this.gameService.board();
    if (!board || !board.mielegrammi) return false;
    return board.mielegrammi.includes(word.toUpperCase());
  }
}