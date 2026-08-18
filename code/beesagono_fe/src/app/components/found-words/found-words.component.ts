import { Component, computed, inject, input, signal } from '@angular/core';
import { WordMapComponent } from '../word-map/word-map.component';
import { GameService } from '../../services/game/game.service';

@Component({
  selector: 'app-found-words',
  standalone: true,
  imports: [WordMapComponent],
  templateUrl: './found-words.component.html',
  styleUrl: './found-words.component.scss',
})
export class FoundWordsComponent {
  gameService = inject(GameService);

  // Signal inputs for found words,  optional pangrams (Mielegrammi), and total counts
  foundWords = input<string[]>([]);
  foundMielegrammi = input<string[]>([]);
  totalPossibleWords = input<number>(0);
  totalPossibleMielegrammi = input<number>(0);

  // Local state for toggling section visibility on smaller viewports
  isExpanded = signal<boolean>(false);

  showMap = signal<boolean>(false);

  // Set lookup helper to identify pangrams efficiently
  private readonly mielegrammiSet = computed(
    () => new Set(this.foundMielegrammi().map((word) => word.toUpperCase()))
  );

  // Checks whether a given word is classified as a Mielegramma (pangram)
  isMielegramma(word: string): boolean {
    return this.mielegrammiSet().has(word.toUpperCase());
  }

  // Toggles the accordion expansion state
  toggleExpanded(): void {
    this.isExpanded.update((prev) => !prev);
  }

  toggleMapView(event: Event) {
    event.stopPropagation();
    if (!this.isExpanded()) {
      this.isExpanded.set(true);
    }
    this.showMap.update((prev) => !prev);
  }
}