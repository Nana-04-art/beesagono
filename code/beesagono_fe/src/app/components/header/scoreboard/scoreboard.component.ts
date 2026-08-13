import { Component, computed, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../../services/game/game.service';
import { ScoreService } from '../../../services/score/score.service';
import { RANK_TIERS } from '../../../config/rank-tiers.config';

@Component({
  selector: 'app-scoreboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scoreboard.component.html',
  styleUrl: './scoreboard.component.scss',
})
export class ScoreboardComponent {
  private readonly gameService = inject(GameService);
  private readonly scoreService = inject(ScoreService);

  // Inputs received from the parent (with fallback to GameService signals)
  scoreInput = input<number | undefined>(undefined, { alias: 'score' });
  rankNameInput = input<string | undefined>(undefined, { alias: 'rankName' });

  // Actual score
  score = computed(() => {
    const inputVal = this.scoreInput();
    return inputVal !== undefined ? inputVal : this.gameService.score();
  });

  // Actual rank name
  rankName = computed(() => {
    const inputVal = this.rankNameInput();
    return inputVal !== undefined ? inputVal : this.gameService.rank().label;
  });

  maxScore = computed(() => this.gameService.maxScore());

  // Calculation of the fill percentage (0% - 100%)
  progressPercentage = computed(() => {
    const max = this.maxScore();
    if (!max || max === 0) return 0;
    return Math.min(Math.max((this.score() / max) * 100, 0), 100);
  });

  // Find the next level to reach
  nextRankTier = computed(() => {
    const currentScore = this.score();
    const max = this.maxScore();
    return RANK_TIERS.find((t) => Math.ceil((max * t.threshold) / 100) > currentScore);
  });

  // Points needed to reach the next rank (calculated based on nextRankTier)
  pointsToNextRank = computed(() => {
    const next = this.nextRankTier();
    if (!next) return 0;
    const requiredForNext = Math.ceil((this.maxScore() * next.threshold) / 100);
    return Math.max(requiredForNext - this.score(), 0);
  });

  // Current rank emoji
  currentRankEmoji = computed(() => {
    const label = this.rankName();
    return label ? label.split(' ')[0] : '🐝';
  });

  nextRankEmoji = computed(() => {
    const next = this.nextRankTier();
    return next ? next.label.split(' ')[0] : null;
  });

  nextRankThreshold = computed(() => {
    const next = this.nextRankTier();
    return next ? next.threshold : 100;
  });

  // Map of all ranks with required points and unlocked status
  rankTiersWithPoints = computed(() => {
    const max = this.maxScore();
    const currentScore = this.score();

    return RANK_TIERS.map((tier) => {
      const requiredPoints = Math.ceil((max * tier.threshold) / 100);
      return {
        ...tier,
        requiredPoints,
        isUnlocked: currentScore >= requiredPoints,
        isCurrent: this.rankName().includes(tier.label.split(' ').slice(1).join(' ')) ||
          this.rankName() === tier.label,
      };
    });
  });
}