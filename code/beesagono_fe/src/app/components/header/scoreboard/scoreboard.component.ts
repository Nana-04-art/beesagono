import { Component, computed, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ScoreService } from '../../../services/score/score.service';
import { RANK_TIERS } from '../../../config/rank-tiers.config';
import { GameFacadeService } from '../../../services/game/game-facade.service';

@Component({
  selector: 'app-scoreboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scoreboard.component.html',
  styleUrl: './scoreboard.component.scss',
})
export class ScoreboardComponent {
  private readonly gameFacade = inject(GameFacadeService);
  private readonly scoreService = inject(ScoreService);

  // Optional Signal Inputs (permette l'uso sia isolato/custom che integrato con Facade)
  scoreInput = input<number | undefined>(undefined, { alias: 'score' });
  rankNameInput = input<string | undefined>(undefined, { alias: 'rankName' });

  // Effective Score & MaxScore
  readonly score = computed(() => this.scoreInput() ?? this.gameFacade.score());
  readonly maxScore = computed(() => this.gameFacade.maxScore());

  // Current Rank info
  readonly currentRank = computed(() => this.gameFacade.rank());
  readonly rankName = computed(() => this.rankNameInput() ?? this.currentRank().label);

  // Progress Percentage (0% - 100%)
  readonly progressPercentage = computed(() =>
    this.scoreService.calculatePercentage(this.score(), this.maxScore())
  );

  // Next Rank calculations
  readonly nextRankTier = computed(() =>
    this.scoreService.getNextRank(this.score(), this.maxScore())
  );

  // Points needed to reach the next rank (calculated based on nextRankTier)
  pointsToNextRank = computed(() =>
    this.scoreService.getPointsToNext(this.score(), this.maxScore())
  );

  // Current rank emoji
  readonly currentRankEmoji = computed(() => this.rankName().split(' ')[0] ?? '🐝');

  readonly nextRankEmoji = computed(() => {
    const next = this.nextRankTier();
    return next ? next.label.split(' ')[0] : null;
  });

  // Percentage threshold for the next level
  readonly nextRankThreshold = computed(() => {
    const next = this.nextRankTier();
    return next ? next.threshold : 100;
  });

  // Map of all ranks with required points and unlocked status
  rankTiersWithPoints = computed(() => {
    const max = this.maxScore();
    const currentScore = this.score();
    const currentRankLabel = this.rankName();

    return RANK_TIERS.map((tier) => {
      const requiredPoints = Math.ceil((max * tier.threshold) / 100);
      return {
        ...tier,
        requiredPoints,
        isUnlocked: currentScore >= requiredPoints,
        isCurrent: currentRankLabel === tier.label || currentRankLabel.includes(tier.label),
      };
    });
  });
}