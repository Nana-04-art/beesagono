import { Component, ElementRef, HostListener, computed, inject, output, signal } from '@angular/core';
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
  protected gameService = inject(GameService);
  private elementRef = inject(ElementRef);
  
  readonly rank = this.gameService.rank;
  readonly score = this.gameService.score;

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

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.closeRules();
    }
  }

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