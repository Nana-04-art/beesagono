import { Component, computed, inject, signal } from '@angular/core';
import { GameService } from '../../services/game/game.service';

@Component({
  selector: 'app-invalid-words',
  imports: [],
  templateUrl: './invalid-words.component.html',
  styleUrl: './invalid-words.component.scss',
})
export class InvalidWordsComponent {
  private gameService = inject(GameService);

  // Shutter open/closed status (open by default)
  readonly isExpanded = signal<boolean>(true);

  // List of attempted invalid words
  readonly invalidWords = computed(() => this.gameService.invalidWords());

  toggleExpand(): void {
    this.isExpanded.update((prev) => !prev);
  }
}
