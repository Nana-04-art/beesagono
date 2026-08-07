import { Component, computed, effect, inject, signal } from '@angular/core';
import { GameService } from '../../services/game/game.service';
import { LetterGroup } from '../../models/letter-group.model';
import { NgClass } from '@angular/common';

@Component({
  selector: 'app-words-by-letter',
  imports: [NgClass],
  templateUrl: './words-by-letter.component.html',
  styleUrl: './words-by-letter.component.scss',
})
export class WordsByLetterComponent {
  private gameService = inject(GameService);

  private readonly _expandedLetters = signal<Set<string>>(new Set());
  readonly expandedLetters = this._expandedLetters.asReadonly();

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

    return board.cells.map((cell) => {
      const letter = cell.letter;

      const totalWordsForLetter = possibleWords.filter((w) => w.startsWith(letter));
      const foundWordsForLetter = foundWords.filter((w) => w.startsWith(letter));

      const totalPangramsForLetter = totalWordsForLetter.filter((w) => mielegrammiSet.has(w)).length;
      const foundPangramsForLetter = foundWordsForLetter.filter((w) => foundMielegrammiSet.has(w)).length;

      return {
        letter,
        isCenter: cell.isCenter,
        foundCount: foundWordsForLetter.length,
        totalCount: totalWordsForLetter.length,
        foundPangrams: foundPangramsForLetter,
        totalPangrams: totalPangramsForLetter,
        foundWords: foundWordsForLetter,
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

  isMielegramma(word: string): boolean {
    const board = this.gameService.board();
    if (!board || !board.mielegrammi) return false;
    return board.mielegrammi.includes(word.toUpperCase());
  }
}