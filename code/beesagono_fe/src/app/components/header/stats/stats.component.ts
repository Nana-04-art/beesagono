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

  // Local statistical data from client storage
  readonly localStats = this.statsService.stats;
  readonly currentTier = this.statsService.currentTier;

  readonly gamesPlayed = computed(() => this.localStats().gamesPlayed);
  readonly gamesCompleted = computed(() => this.localStats().gamesCompleted);

  // Streaks
  readonly currentStreak = computed(() => this.localStats().currentStreak);
  readonly maxStreak = computed(() => this.localStats().maxStreak);

  // Completion rate (0% - 100%)
  readonly completionRate = computed(() => {
    const played = this.gamesPlayed();
    if (!played || played === 0) return 0;
    return Math.round((this.gamesCompleted() / played) * 100);
  });

  // Local rank distribution map
  readonly effectiveRankDistribution = computed<Record<string, number>>(() => 
    this.localStats()?.dailyRankDistribution ?? {}
  );

  // Current local season extracted from stats
  readonly currentSeason = computed(() => this.localStats().currentSeason);
}