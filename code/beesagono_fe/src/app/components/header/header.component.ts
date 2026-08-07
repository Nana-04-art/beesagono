import { Component, computed, inject, output } from '@angular/core';
import { GameService } from '../../services/game/game.service';
import { ScoreboardComponent } from '../scoreboard/scoreboard.component';

@Component({
  selector: 'app-header',
  imports: [ScoreboardComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private gameService = inject(GameService);

  // Event emitted when user clicks on help/rules icon
  readonly helpRequested = output<void>();

  // Event emitted when user clicks on stats icon
  readonly statsRequested = output<void>();

  // Event emitted when user clicks on share button
  readonly shareRequested = output<void>();

  // Computes current rank tier from GameService
  readonly currentRank = computed(() => this.gameService.rank());
  readonly score = computed(() => this.gameService.score());
  readonly maxScore = computed(() => this.gameService.board()?.maxScore ?? 100);

  // Computes an emoji icon according to the current rank's threshold
  readonly rankIcon = computed(() => {
    const threshold = this.currentRank().threshold;
    if (threshold >= 100) return '🐝';
    if (threshold >= 70) return '👑';
    if (threshold >= 40) return '🧠';
    if (threshold >= 25) return '⭐';
    if (threshold >= 15) return '💡';
    if (threshold >= 8) return '🚀';
    if (threshold >= 5) return '🐣';
    if (threshold >= 2) return '🍃';
    return '🌱';
  });

  // Computes localized today's date label (e.g., "5 AGOSTO 2026") 
  readonly formattedDate = computed(() => {
    const board = this.gameService.board();
    if (!board) return '';

    const [year, month, day] = board.date.split('-').map(Number);
    const dateObj = new Date(year, month - 1, day);

    return dateObj.toLocaleDateString('it-IT', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).toUpperCase();
  });

  onLogoClick(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}