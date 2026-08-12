import { Component, HostListener, signal, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RANK_TIERS } from '../../config/rank-tiers.config';
import { ScoreboardComponent } from '../scoreboard/scoreboard.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, ScoreboardComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  // Import the degrees for the @for loop in the HTML.
  readonly rankTiers = RANK_TIERS;

  readonly isScoreboardOpen = signal<boolean>(false);
  readonly isRulesOpen = signal<boolean>(false);

  readonly score = input<number>(0);
  readonly rank = input<{ label: string }>({ label: '🌱 Iniziato' });
  readonly formattedDate = input<string>('');

  readonly statsRequested = output<void>();
  readonly shareRequested = output<void>();
  readonly logoClicked = output<void>();

  toggleScoreboard(): void {
    this.isScoreboardOpen.update((open) => !open);
  }

  toggleRules(): void {
    this.isRulesOpen.update((open) => !open);
  }

  closeRules(): void {
    this.isRulesOpen.set(false);
  }

  onLogoClick(): void {
    this.logoClicked.emit();
  }

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    this.isScoreboardOpen.set(false);
    this.isRulesOpen.set(false);
  }
}