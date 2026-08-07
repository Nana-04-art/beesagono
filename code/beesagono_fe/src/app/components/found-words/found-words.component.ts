import { Component, computed, input, signal } from '@angular/core';

@Component({
  selector: 'app-found-words',
  standalone: true,
  imports: [],
  templateUrl: './found-words.component.html',
  styleUrl: './found-words.component.scss',
})
export class FoundWordsComponent {
  // Signal inputs for found words,  optional pangrams (Mielegrammi), and total counts
  foundWords = input.required<string[]>();
  foundMielegrammi = input<string[]>([]);
  totalPossibleWords = input<number>(0);
  totalPossibleMielegrammi = input<number>(0);

  // Local state for toggling section visibility on smaller viewports
  isExpanded = signal<boolean>(false);

  // Computed total count of all discovered words
  readonly totalCount = computed(
    () => this.foundWords().length + this.foundMielegrammi().length
  );

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

  readonly totalFoundCount = computed(
    () => this.foundWords().length + this.foundMielegrammi().length
  );
}