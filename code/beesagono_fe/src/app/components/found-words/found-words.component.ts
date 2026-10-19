import { Component, computed, inject, input, signal } from '@angular/core';
import { WordMapComponent } from '../word-map/word-map.component';
import { GameFacadeService } from '../../services/game/game-facade.service';
import { WordMapItem } from '../../models/game/word-map-item.model';

@Component({
  selector: 'app-found-words',
  standalone: true,
  imports: [WordMapComponent],
  templateUrl: './found-words.component.html',
  styleUrl: './found-words.component.scss',
})
export class FoundWordsComponent {
  readonly gameFacade = inject(GameFacadeService);

  foundWordsInput = input<string[] | undefined>(undefined, { alias: 'foundWords' });
  foundMielegrammiInput = input<string[] | undefined>(undefined, { alias: 'foundMielegrammi' });
  totalPossibleWordsInput = input<number | undefined>(undefined, { alias: 'totalPossibleWords' });
  totalPossibleMielegrammiInput = input<number | undefined>(undefined, { alias: 'totalPossibleMielegrammi' });

  // Effective Values (Automatic fallback to GameFacadeService if inputs are not passed)
  readonly foundWords = computed(() => this.foundWordsInput() ?? this.gameFacade.foundWords());
  readonly foundMielegrammi = computed(() => this.foundMielegrammiInput() ?? this.gameFacade.foundMielegrammi());
  readonly totalPossibleWords = computed(() => this.totalPossibleWordsInput() ?? this.gameFacade.totalPossibleWords());
  readonly totalPossibleMielegrammi = computed(() => this.totalPossibleMielegrammiInput() ?? this.gameFacade.totalMielegrammi());

  // UI Local state
  readonly isExpanded = signal<boolean>(false);
  readonly showMap = signal<boolean>(false);

  // Efficient Lookup Set for immediate verification of Mielegrammi (pangrams)
  private readonly mielegrammiSet = computed(
    () => new Set(this.foundMielegrammi().map((word) => word.toUpperCase()))
  );

  // Secure generation of the global map for the overview (found words + grey placeholders up to the total)
  readonly wordMap = computed<WordMapItem[]>(() => {
    const found = this.foundWords();
    const totalCount = this.totalPossibleWords();
    const items: WordMapItem[] = [];

    // Adds found words first, sorted by length and then alphabetically
    const sortedFound = [...found].sort((a, b) => {
      if (a.length !== b.length) return a.length - b.length;
      return a.localeCompare(b);
    });

    sortedFound.forEach((word) => {
      const upperWord = word.toUpperCase();
      const initial = upperWord[0];
      const isPangram = this.isMielegramma(upperWord);
      items.push({
        word: upperWord,
        length: upperWord.length,
        initial,
        isFound: true,
        isPangram
      });
    });

    // Adds grey placeholders to reach the safe total count
    const missingCount = Math.max(0, totalCount - sortedFound.length);
    for (let i = 0; i < missingCount; i++) {
      items.push({
        word: `HIDDEN_GLOBAL_${i}`,
        length: 4,
        initial: '?',
        isFound: false,
        isPangram: false
      });
    }

    return items;
  });

  // Checks whether a given word is classified as a Mielegramma (pangram) using the Set helper
  isMielegramma(word: string): boolean {
    return this.mielegrammiSet().has(word.toUpperCase());
  }

  // Toggles the accordion expansion state
  toggleExpanded(): void {
    this.isExpanded.update((prev) => !prev);
  }

  toggleMapView(event: Event): void {
    event.stopPropagation();
    if (!this.isExpanded()) {
      this.isExpanded.set(true);
    }
    this.showMap.update((prev) => !prev);
  }
}