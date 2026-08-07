import { Component, computed, inject, output, signal, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game/game.service';
import { ScoreboardComponent } from '../scoreboard/scoreboard.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, ScoreboardComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  private gameService = inject(GameService);
  private elementRef = inject(ElementRef);

  readonly helpRequested = output<void>();
  readonly statsRequested = output<void>();
  readonly shareRequested = output<void>();

  readonly isRulesOpen = signal<boolean>(false);

  toggleRules(): void {
    this.isRulesOpen.update(value => !value);
  }

  closeRules(): void {
    this.isRulesOpen.set(false);
  }

  // Closes the popover when the user clicks anywhere outside the component.
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.closeRules();
    }
  }

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