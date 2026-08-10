import { Component, computed, inject, input } from '@angular/core';
import { GameService } from '../../services/game/game.service';
import { RANK_TIERS } from '../../config/rank-tiers.config';

@Component({
  selector: 'app-scoreboard',
  imports: [],
  templateUrl: './scoreboard.component.html',
  styleUrl: './scoreboard.component.scss',
})
export class ScoreboardComponent {
  private gameService = inject(GameService);

  // Current player score
  score = input<number>(0);

  // Current Level/Rank (es. "Inizio", "Buono", "Genio")
  rankName = input<string>();

  maxScore = computed(() => this.gameService.maxScore());

  // Points needed to reach the next level
  nextRankScore = computed(() => this.gameService.nextRankScore());

  // Calculate the actual fill percentage (from 0% to 100%)
  progressPercentage = computed(() => {
    const max = this.maxScore();
    if (max === 0) return 0;
    return Math.min(Math.max((this.score() / max) * 100, 0), 100);
  });

  /**
   * Points needed to reach the next rank.
   * Returns 0 if the player has already reached or exceeded the target value.
   */
  pointsToNextRank = computed(() => {
    return Math.max(this.nextRankScore() - this.score(), 0);
  });

  // Current rank emoji
  currentRankEmoji = computed(() => {
    return this.gameService.rank().label.split(' ')[0];
  });

  // Find the next rank tier for the target emoji
  nextRankTier = computed(() => {
    const currentScore = this.score();
    const max = this.maxScore();
    return RANK_TIERS.find(t => Math.ceil((max * t.threshold) / 100) > currentScore);
  });

  nextRankEmoji = computed(() => {
    const next = this.nextRankTier();
    return next ? next.label.split(' ')[0] : null;
  });

  nextRankThreshold = computed(() => {
    const next = this.nextRankTier();
    return next ? next.threshold : 100;
  });

  // Map all grades with their respective percentages and exact target scores
  rankTiersWithPoints = computed(() => {
    const max = this.maxScore();
    return RANK_TIERS.map((tier) => {
      const requiredPoints = Math.ceil((max * tier.threshold) / 100);
      return {
        ...tier,
        requiredPoints,
        isUnlocked: this.score() >= requiredPoints,
      };
    });
  });
}
