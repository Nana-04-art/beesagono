import { Component, HostListener, signal, input, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RANK_TIERS } from '../../config/rank-tiers.config';
import { ScoreboardComponent } from './scoreboard/scoreboard.component';
import { StatsComponent } from './stats/stats.component';
import { RulesComponent } from './rules/rules.component';
import { ThemeService } from '../../services/theme/theme.service';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth/auth.service';

export type HeaderPopover = 'scoreboard' | 'rules' | 'stats' | 'userMenu' | null;

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, ScoreboardComponent, StatsComponent, RulesComponent, RouterLink],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {

  public readonly themeService = inject(ThemeService);
  public readonly authService = inject(AuthService);

  // Import the degrees for the @for loop in the HTML.
  readonly rankTiers = RANK_TIERS;

  // Single Signal to manage exclusive opening.
  readonly activePopover = signal<HeaderPopover>(null);

  readonly score = input<number>(0);
  readonly rank = input<{ label: string }>({ label: '🌱 Iniziato' });
  readonly formattedDate = input<string>('');

  readonly statsRequested = output<void>();
  readonly shareRequested = output<void>();
  readonly logoClicked = output<void>();

  toggleUserMenu(): void {
    this.activePopover.update((curr) => (curr === 'userMenu' ? null : 'userMenu'));
  }

  toggleScoreboard(): void {
    this.activePopover.update((curr) => (curr === 'scoreboard' ? null : 'scoreboard'));
  }

  toggleRules(): void {
    this.activePopover.update((curr) => (curr === 'rules' ? null : 'rules'));
  }

  toggleStats(): void {
    this.activePopover.update((curr) => (curr === 'stats' ? null : 'stats'));
  }

  closeAll(): void {
    this.activePopover.set(null);
  }

  onLogoClick(): void {
    this.logoClicked.emit();
  }

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    this.closeAll();
  }
}