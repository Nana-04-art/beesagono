import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatsService } from '../../../services/stats/stats.service';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats.component.html',
  styleUrl: './stats.component.scss',
})
export class StatsComponent {
  private readonly statsService = inject(StatsService);

  readonly stats = this.statsService.stats;
  readonly currentTier = this.statsService.currentTier;

  readonly completionRate = computed(() => {
    const s = this.stats();
    if (s.gamesPlayed === 0) return 0;
    return Math.round((s.gamesCompleted / s.gamesPlayed) * 100);
  });
}